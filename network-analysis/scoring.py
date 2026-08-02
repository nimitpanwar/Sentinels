"""
scoring.py - turns raw per-signal values into a single, explainable,
reproducible 0-100 network_risk_score per account.

Normalization: raw values from different algorithms are NOT directly
comparable (e.g. PageRank values sum to 1 across the whole graph and shrink
as the graph grows) - each signal is converted to a PERCENTILE RANK (0-100)
within the current run before weighting, so "top 2% of accounts on this
signal" means the same thing run to run regardless of graph size.
"""
from __future__ import annotations

import config


def percentile_ranks(values: dict[int, float]) -> dict[int, float]:
    """Maps each account_id to its percentile rank (0-100) among all values given, ties averaged."""
    if not values:
        return {}
    sorted_items = sorted(values.items(), key=lambda kv: kv[1])
    n = len(sorted_items)
    ranks: dict[int, float] = {}
    i = 0
    while i < n:
        j = i
        while j < n and sorted_items[j][1] == sorted_items[i][1]:
            j += 1
        # All tied values get the same, averaged percentile.
        avg_rank = (i + j - 1) / 2
        percentile = 100.0 * avg_rank / max(n - 1, 1)
        for k in range(i, j):
            ranks[sorted_items[k][0]] = percentile
        i = j
    return ranks


def combine_scores(percentiles: dict[str, float]) -> float:
    """Weighted sum of already-percentiled (0-100) signals -> final 0-100 network_risk_score."""
    total_weight = sum(config.SIGNAL_WEIGHTS.values())
    score = sum(percentiles.get(signal, 0.0) * weight for signal, weight in config.SIGNAL_WEIGHTS.items())
    return round(score / total_weight, 2)


def generate_reason(
    fraud_exposure_pct: float,
    seed_neighbor_count: int,
    shared_payee_count: int,
    shared_payee_pct: float,
    community_size: int,
    is_dense: bool,
    growth_pct: float,
) -> str:
    """
    Deterministic template (same inputs -> same sentence), not a free-form
    LLM call - keeps the explanation reproducible and auditable, matching
    the "explainability" requirement.
    """
    parts: list[str] = []

    if fraud_exposure_pct >= 75 and seed_neighbor_count > 0:
        parts.append(
            f"High network exposure score (top {100 - fraud_exposure_pct:.0f}% of accounts) "
            f"due to proximity to {seed_neighbor_count} confirmed-fraud account(s)."
        )
    elif fraud_exposure_pct >= 50:
        parts.append(f"Moderate network exposure score ({fraud_exposure_pct:.0f}th percentile).")

    if shared_payee_count > 0:
        parts.append(
            f"Shares {shared_payee_count} payee relationship(s) with other accounts "
            f"(top {100 - shared_payee_pct:.0f}% by shared-payee concentration)."
        )

    if community_size > 1:
        label = "dense interconnected account group" if is_dense else "small account cluster"
        parts.append(f"Member of a {label} containing {community_size} account(s).")

    if growth_pct >= 75:
        parts.append(f"Rapidly acquiring new payee relationships (top {100 - growth_pct:.0f}% growth rate).")

    if not parts:
        return "No significant network risk signals detected in this run."
    return " ".join(parts)
