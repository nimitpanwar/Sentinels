"""
algorithms.py - the actual network-science computations, kept separate from
scoring/weighting (scoring.py) and DB access (db.py) so each piece is easy
to reason about/replace independently.
"""
from __future__ import annotations

import community as community_louvain  # python-louvain package, imports as "community"
import networkx as nx
import pandas as pd

import config


def payee_concentration(bipartite_graph: nx.Graph) -> dict[int, int]:
    """
    For each account, the max degree (number of distinct accounts) among the
    payees it uses - i.e. "is this account transacting with a payee that a
    lot of OTHER accounts also use". High concentration on a shared payee is
    the strongest single-payee-based fraud signal.
    """
    payee_degree = {
        node[1]: bipartite_graph.degree(node)
        for node in bipartite_graph.nodes
        if bipartite_graph.nodes[node].get("kind") == "payee"
    }
    result: dict[int, int] = {}
    for node in bipartite_graph.nodes:
        if bipartite_graph.nodes[node].get("kind") != "account":
            continue
        account_id = node[1]
        neighbor_payees = [n[1] for n in bipartite_graph.neighbors(node)]
        result[account_id] = max((payee_degree[p] for p in neighbor_payees), default=0)
    return result


def shared_payee_counts(account_graph: nx.Graph) -> dict[int, int]:
    """Total distinct shared-payee edges touching each account (sum of the 'shared_payees' edge attribute)."""
    result: dict[int, int] = {node: 0 for node in account_graph.nodes}
    for u, v, data in account_graph.edges(data=True):
        result[u] += data.get("shared_payees", 1)
        result[v] += data.get("shared_payees", 1)
    return result


def detect_communities(account_graph: nx.Graph) -> dict[int, int]:
    """
    Louvain community detection on the weighted account-account projection.
    Returns {account_id: community_id}. A community here means "a group of
    accounts structurally linked via shared payees" - see graph_builder.py's
    module docstring for why this is NOT the same claim as a literal money
    transfer cycle/chain.
    """
    if account_graph.number_of_edges() == 0:
        return {node: node for node in account_graph.nodes}
    return community_louvain.best_partition(account_graph, weight="weight", random_state=42)


def personalized_pagerank(account_graph: nx.Graph, seed_account_ids: list[int]) -> dict[int, float]:
    """
    Personalized PageRank seeded from confirmed-fraud accounts (see
    db.fetch_confirmed_fraud_account_ids) - "accounts closer to confirmed
    fraud cases inherit higher network risk". If there are no seed accounts
    yet (cold start - no CLOSED/CONFIRMED_FRAUD cases logged), falls back to
    uniform PageRank so the job still produces a meaningful score.
    """
    if account_graph.number_of_nodes() == 0:
        return {}

    seeds_in_graph = [a for a in seed_account_ids if a in account_graph.nodes]
    personalization = None
    if seeds_in_graph:
        personalization = {node: (1.0 if node in seeds_in_graph else 0.0) for node in account_graph.nodes}

    try:
        return nx.pagerank(account_graph, weight="weight", personalization=personalization)
    except nx.PowerIterationFailedConvergence:
        # Extremely large/pathological graphs can fail to converge in the
        # default iteration budget - fall back to an unweighted, uniform
        # PageRank rather than crashing the whole run.
        return nx.pagerank(account_graph)


def relationship_growth(transactions: pd.DataFrame, now: pd.Timestamp, lookback_days: int) -> dict[int, float]:
    """
    "How quickly is this account creating NEW payee relationships" - the
    fraction of an account's distinct payees (within the lookback window)
    that are first seen in the most recent third of that window. A sudden
    spike of brand-new payees is more suspicious than a single new payee.
    """
    if transactions.empty:
        return {}

    window_start = now - pd.Timedelta(days=lookback_days)
    recent_cutoff = now - pd.Timedelta(days=lookback_days / 3)

    result: dict[int, float] = {}
    for account_id, group in transactions.groupby("account_id"):
        first_seen = group.groupby("payee_id")["transaction_timestamp"].min()
        total_payees = len(first_seen)
        if total_payees == 0:
            result[int(account_id)] = 0.0
            continue
        new_payees = (first_seen >= recent_cutoff).sum()
        result[int(account_id)] = new_payees / total_payees
    return result


def is_dense_community(community_sizes: dict[int, int], community_id: int) -> bool:
    return community_sizes.get(community_id, 0) >= config.DENSE_COMMUNITY_MIN_SIZE
