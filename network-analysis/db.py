"""
db.py - all database access for the network-analysis job, in one place
(mirrors the Spring side's "repository" convention: exactly one module is
allowed to talk to the DB directly).

The job is intentionally STATELESS across runs: every call here either
reads fresh data or writes a brand-new row - nothing is cached/remembered
in the Python process between runs. See run_analysis.py for the
read -> build -> compute -> write -> exit lifecycle this supports.
"""
from __future__ import annotations

import datetime as dt
import json
from typing import Optional

import pandas as pd
from sqlalchemy import create_engine, text
from sqlalchemy.engine import Engine

import config


def get_engine() -> Engine:
    return create_engine(config.sqlalchemy_url(), pool_pre_ping=True)


def fetch_transactions_window(engine: Engine, since: dt.datetime) -> pd.DataFrame:
    """All transactions within the lookback window - the raw material for the bipartite graph."""
    query = text("""
        SELECT transaction_id, account_id, payee_id, amount, transaction_timestamp
        FROM transactions
        WHERE transaction_timestamp >= :since
    """)
    with engine.connect() as conn:
        return pd.read_sql(query, conn, params={"since": since})


def fetch_confirmed_fraud_account_ids(engine: Engine) -> list[int]:
    """
    Seed set for personalized PageRank: accounts with at least one CLOSED
    case whose resolution_reason_code is explicitly CONFIRMED_FRAUD (a
    structured field - see enums.ResolutionReasonCode on the Java side).
    Deliberately NOT "any CLOSED case", since CLOSED also covers legitimate/
    resolved-but-not-fraud outcomes.
    """
    query = text("""
        SELECT DISTINCT account_id
        FROM cases
        WHERE status = 'CLOSED' AND resolution_reason_code = 'CONFIRMED_FRAUD'
    """)
    with engine.connect() as conn:
        rows = conn.execute(query).fetchall()
    return [r[0] for r in rows]


def fetch_pending_run_request(engine: Engine) -> Optional[dict]:
    """Oldest PENDING request row, if any (operator clicked 'Run Analysis Now')."""
    query = text("""
        SELECT id, lookback_days
        FROM network_run_requests
        WHERE status = 'PENDING'
        ORDER BY requested_at ASC
        LIMIT 1
    """)
    with engine.connect() as conn:
        row = conn.execute(query).mappings().first()
    return dict(row) if row else None


def mark_run_request_done(engine: Engine, request_id: int) -> None:
    query = text("""
        UPDATE network_run_requests
        SET status = 'DONE', picked_up_at = :now
        WHERE id = :id
    """)
    with engine.begin() as conn:
        conn.execute(query, {"id": request_id, "now": dt.datetime.utcnow()})


def start_run(engine: Engine, lookback_days: int, trigger_type: str) -> int:
    """Inserts a RUNNING network_runs row and returns its run_id."""
    query = text("""
        INSERT INTO network_runs (started_at, status, trigger_type, lookback_days, algorithm_version)
        VALUES (:started_at, 'RUNNING', :trigger_type, :lookback_days, :algorithm_version)
    """)
    with engine.begin() as conn:
        result = conn.execute(query, {
            "started_at": dt.datetime.utcnow(),
            "trigger_type": trigger_type,
            "lookback_days": lookback_days,
            "algorithm_version": config.ALGORITHM_VERSION,
        })
        return result.lastrowid


def complete_run(engine: Engine, run_id: int, accounts_analyzed: int, accounts_flagged: int) -> None:
    query = text("""
        UPDATE network_runs
        SET status = 'COMPLETED', completed_at = :completed_at,
            accounts_analyzed = :accounts_analyzed, accounts_flagged = :accounts_flagged
        WHERE run_id = :run_id
    """)
    with engine.begin() as conn:
        conn.execute(query, {
            "run_id": run_id,
            "completed_at": dt.datetime.utcnow(),
            "accounts_analyzed": accounts_analyzed,
            "accounts_flagged": accounts_flagged,
        })


def fail_run(engine: Engine, run_id: int, error_message: str) -> None:
    query = text("""
        UPDATE network_runs
        SET status = 'FAILED', completed_at = :completed_at, error_message = :error_message
        WHERE run_id = :run_id
    """)
    with engine.begin() as conn:
        conn.execute(query, {
            "run_id": run_id,
            "completed_at": dt.datetime.utcnow(),
            "error_message": error_message[:2000],
        })


def insert_account_scores(engine: Engine, run_id: int, rows: list[dict]) -> None:
    """
    Bulk-inserts one row per account for this run into account_network_scores.
    Append-only by design (see entity.AccountNetworkScore on the Java side) -
    never UPDATEs a previous run's row, so score history/timelines stay intact.
    """
    if not rows:
        return
    query = text("""
        INSERT INTO account_network_scores (
            run_id, account_id, network_risk_score, page_rank_percentile,
            shared_payee_count, community_id, community_size, growth_score,
            fraud_exposure_score, evidence_json, network_reason, computed_at
        ) VALUES (
            :run_id, :account_id, :network_risk_score, :page_rank_percentile,
            :shared_payee_count, :community_id, :community_size, :growth_score,
            :fraud_exposure_score, :evidence_json, :network_reason, :computed_at
        )
    """)
    now = dt.datetime.utcnow()
    payload = []
    for row in rows:
        payload.append({
            "run_id": run_id,
            "account_id": row["account_id"],
            "network_risk_score": row["network_risk_score"],
            "page_rank_percentile": row["page_rank_percentile"],
            "shared_payee_count": row["shared_payee_count"],
            "community_id": row["community_id"],
            "community_size": row["community_size"],
            "growth_score": row["growth_score"],
            "fraud_exposure_score": row["fraud_exposure_score"],
            "evidence_json": json.dumps(row["evidence"]),
            "network_reason": row["network_reason"],
            "computed_at": now,
        })
    with engine.begin() as conn:
        conn.execute(query, payload)
