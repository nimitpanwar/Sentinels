"""
graph_builder.py - builds the bipartite Account<->Payee graph from the
transactions window, then projects it onto a weighted Account<->Account
graph (two accounts are connected if they share a payee).

IMPORTANT nuance (schema-grounded, not an assumption): the transactions
table only has account_id -> payee_id edges - there is NO account_id ->
account_id edge anywhere in the schema. So the account-account graph built
here is a PROJECTION through shared payees, not a literal money-transfer
graph. Treat anything derived from it (e.g. dense communities) as
"structurally similar/interconnected accounts", never as "circular
transactions" or "transfer chains" - see notes/RiskLogic.md's corrected
framing.
"""
from __future__ import annotations

import math
from collections import defaultdict

import networkx as nx
import pandas as pd


def build_bipartite_graph(transactions: pd.DataFrame) -> nx.Graph:
    """Nodes are ('A', account_id) and ('P', payee_id); edges are individual transactions."""
    graph = nx.Graph()
    for row in transactions.itertuples(index=False):
        a_node = ("A", int(row.account_id))
        p_node = ("P", int(row.payee_id))
        graph.add_node(a_node, kind="account", account_id=int(row.account_id))
        graph.add_node(p_node, kind="payee", payee_id=int(row.payee_id))
        graph.add_edge(a_node, p_node)
    return graph


def project_to_account_graph(transactions: pd.DataFrame, now: pd.Timestamp, lookback_days: int) -> nx.Graph:
    """
    Weighted account-account projection: an edge between two accounts exists
    per shared payee, weighted by:

        weight = 0.6 * frequency_score + 0.4 * recency_score

    where frequency_score is normalized transaction count on that shared
    payee (log-dampened by amount, so a handful of large legitimate
    transactions don't dominate), and recency_score favors payees used
    recently within the lookback window over ones only used at its start.
    """
    account_graph = nx.Graph()

    # payee_id -> list of (account_id, timestamp, amount)
    payee_activity: dict[int, list[tuple[int, pd.Timestamp, float]]] = defaultdict(list)
    for row in transactions.itertuples(index=False):
        payee_activity[int(row.payee_id)].append(
            (int(row.account_id), pd.Timestamp(row.transaction_timestamp), float(row.amount))
        )

    window_start = now - pd.Timedelta(days=lookback_days)
    window_span_seconds = max((now - window_start).total_seconds(), 1.0)

    for payee_id, activity in payee_activity.items():
        accounts_on_payee: dict[int, list[tuple[pd.Timestamp, float]]] = defaultdict(list)
        for account_id, ts, amount in activity:
            accounts_on_payee[account_id].append((ts, amount))

        account_ids = list(accounts_on_payee.keys())
        if len(account_ids) < 2:
            continue  # payee only used by one account - no shared-payee edge to project

        for i in range(len(account_ids)):
            for j in range(i + 1, len(account_ids)):
                acc_a, acc_b = account_ids[i], account_ids[j]
                weight = _edge_weight(accounts_on_payee[acc_a], window_start, window_span_seconds) + \
                    _edge_weight(accounts_on_payee[acc_b], window_start, window_span_seconds)

                account_graph.add_node(acc_a, account_id=acc_a)
                account_graph.add_node(acc_b, account_id=acc_b)
                if account_graph.has_edge(acc_a, acc_b):
                    account_graph[acc_a][acc_b]["weight"] += weight
                    account_graph[acc_a][acc_b]["shared_payees"] += 1
                else:
                    account_graph.add_edge(acc_a, acc_b, weight=weight, shared_payees=1)

    return account_graph


def _edge_weight(activity: list[tuple[pd.Timestamp, float]], window_start: pd.Timestamp, window_span_seconds: float) -> float:
    frequency_score = math.log1p(len(activity)) * math.log1p(sum(a for _, a in activity))
    most_recent = max(ts for ts, _ in activity)
    recency_score = max((most_recent - window_start).total_seconds(), 0.0) / window_span_seconds
    return 0.6 * frequency_score + 0.4 * recency_score
