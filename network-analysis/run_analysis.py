"""
run_analysis.py - the network-analysis batch job's single entrypoint.

Usage (run manually / from Windows Task Scheduler / cron - see scheduler.py
for the polling-loop wrapper that also handles operator "Run Analysis Now"
requests):

    python run_analysis.py --lookback-days 30 --trigger SCHEDULED

Lifecycle per run (deliberately stateless - see db.py docstring):

    read transactions window
      -> build bipartite + projected account graph
      -> compute signals (concentration, communities, personalized PageRank, growth)
      -> combine into a weighted, percentile-normalized network_risk_score
      -> write one row per account to account_network_scores
      -> mark the network_runs row COMPLETED (or FAILED on any exception)
      -> exit

Never leaves the DB in a half-written state: all per-account rows for a run
are inserted in one go after every signal has been computed in memory, and
any exception anywhere marks the run FAILED with the error message attached
rather than leaving a partially-written run masquerading as complete.
"""
from __future__ import annotations

import argparse
import traceback
from collections import Counter

import pandas as pd

import algorithms
import config
import db
import graph_builder
import scoring


def run(lookback_days: int, trigger_type: str) -> int:
    engine = db.get_engine()
    run_id = db.start_run(engine, lookback_days, trigger_type)
    print(f"[network-analysis] started run_id={run_id} lookback_days={lookback_days} trigger={trigger_type}")

    try:
        now = pd.Timestamp.utcnow().tz_localize(None)
        since = now - pd.Timedelta(days=lookback_days)

        transactions = db.fetch_transactions_window(engine, since)
        all_account_ids = set(int(a) for a in transactions["account_id"].unique()) if not transactions.empty else set()

        if not all_account_ids:
            print("[network-analysis] no transactions in lookback window - nothing to analyze")
            db.complete_run(engine, run_id, accounts_analyzed=0, accounts_flagged=0)
            return run_id

        bipartite_graph = graph_builder.build_bipartite_graph(transactions)
        account_graph = graph_builder.project_to_account_graph(transactions, now, lookback_days)
        seed_accounts = db.fetch_confirmed_fraud_account_ids(engine)

        # payee_concentration (max fan-in of any single payee this account uses) is the
        # weighted "shared_payee_concentration" signal - matches notes/RiskLogic.md's
        # "Payee Concentration" bullet directly. shared_counts (total distinct shared-payee
        # edges) is kept separately purely as displayed evidence (its own DB column), not
        # double-counted into the weighted score.
        concentration = _fill_missing(algorithms.payee_concentration(bipartite_graph), all_account_ids)
        shared_counts = _fill_missing(algorithms.shared_payee_counts(account_graph), all_account_ids)
        communities = algorithms.detect_communities(account_graph) if account_graph.number_of_nodes() else {}
        # Accounts absent from account_graph (no shared payees with anyone) get a distinct
        # negative sentinel community id derived from their own account_id - Louvain's
        # community ids are always >= 0, so this can never collide with a real community
        # and wrongly inflate its size/evidence.
        communities = {aid: communities.get(aid, -aid - 1) for aid in all_account_ids}
        community_sizes = Counter(communities.values())
        pagerank = _fill_missing(algorithms.personalized_pagerank(account_graph, seed_accounts), all_account_ids)
        growth = _fill_missing(algorithms.relationship_growth(transactions, now, lookback_days), all_account_ids)

        concentration_pct = scoring.percentile_ranks(concentration)
        pagerank_pct = scoring.percentile_ranks(pagerank)
        growth_pct = scoring.percentile_ranks(growth)
        community_size_by_account = {aid: community_sizes[communities[aid]] for aid in all_account_ids}
        community_size_pct = scoring.percentile_ranks(community_size_by_account)

        seed_set = set(seed_accounts)
        rows = []
        accounts_flagged = 0
        for account_id in all_account_ids:
            is_dense = community_size_by_account[account_id] >= config.DENSE_COMMUNITY_MIN_SIZE
            percentiles = {
                "fraud_exposure": pagerank_pct.get(account_id, 0.0),
                "shared_payee_concentration": concentration_pct.get(account_id, 0.0),
                "community_membership": community_size_pct.get(account_id, 0.0),
                "relationship_growth": growth_pct.get(account_id, 0.0),
                "dense_cluster": 100.0 if is_dense else 0.0,
            }
            final_score = scoring.combine_scores(percentiles)
            if final_score >= config.FLAGGED_SCORE_THRESHOLD:
                accounts_flagged += 1

            seed_neighbor_count = len(
                set(account_graph.neighbors(account_id)) & seed_set
            ) if account_graph.has_node(account_id) else 0

            reason = scoring.generate_reason(
                fraud_exposure_pct=percentiles["fraud_exposure"],
                seed_neighbor_count=seed_neighbor_count,
                shared_payee_count=shared_counts.get(account_id, 0),
                shared_payee_pct=percentiles["shared_payee_concentration"],
                community_size=community_size_by_account[account_id],
                is_dense=is_dense,
                growth_pct=percentiles["relationship_growth"],
            )

            rows.append({
                "account_id": account_id,
                "network_risk_score": final_score,
                "page_rank_percentile": round(percentiles["fraud_exposure"], 2),
                "shared_payee_count": shared_counts.get(account_id, 0),
                "community_id": communities[account_id],
                "community_size": community_size_by_account[account_id],
                "growth_score": round(percentiles["relationship_growth"], 2),
                "fraud_exposure_score": round(percentiles["fraud_exposure"], 2),
                "evidence": {
                    "page_rank_percentile": round(percentiles["fraud_exposure"], 2),
                    "shared_payees": shared_counts.get(account_id, 0),
                    "shared_payee_percentile": round(percentiles["shared_payee_concentration"], 2),
                    "community_size": community_size_by_account[account_id],
                    "is_dense_community": is_dense,
                    "growth_percentile": round(percentiles["relationship_growth"], 2),
                    "confirmed_fraud_neighbor_count": seed_neighbor_count,
                },
                "network_reason": reason,
            })

        db.insert_account_scores(engine, run_id, rows)
        db.complete_run(engine, run_id, accounts_analyzed=len(all_account_ids), accounts_flagged=accounts_flagged)
        print(f"[network-analysis] completed run_id={run_id} accounts_analyzed={len(all_account_ids)} accounts_flagged={accounts_flagged}")
        return run_id

    except Exception as exc:  # noqa: BLE001 - deliberately broad: any failure must mark the run FAILED, never crash silently
        error_message = f"{exc}\n{traceback.format_exc()}"
        db.fail_run(engine, run_id, error_message)
        print(f"[network-analysis] FAILED run_id={run_id}: {exc}")
        raise


def _fill_missing(values: dict[int, float], universe: set[int]) -> dict[int, float]:
    """Ensures every account in the analyzed universe has an entry (0.0 if absent), so percentile ranks reflect the full population."""
    return {aid: values.get(aid, 0.0) for aid in universe}


def main() -> None:
    parser = argparse.ArgumentParser(description="Sentinel network-analysis batch job")
    parser.add_argument("--lookback-days", type=int, default=config.DEFAULT_LOOKBACK_DAYS)
    parser.add_argument("--trigger", choices=["SCHEDULED", "MANUAL"], default="SCHEDULED")
    args = parser.parse_args()
    run(args.lookback_days, args.trigger)


if __name__ == "__main__":
    main()
