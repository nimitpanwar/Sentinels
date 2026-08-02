"""
config.py - configuration for the network-analysis batch job.

All DB credentials are read from environment variables (via a local .env
file, never committed - see .env.example) rather than hardcoded, and the
recommended setup is a DEDICATED, least-privilege MySQL user (SELECT-only on
transactions/accounts/payees/cases, INSERT/UPDATE-only on the three
network_* tables) rather than reusing the Spring app's root credentials.

ALGORITHM_VERSION is bumped whenever the scoring formula/weights change, so
historical account_network_scores rows stay traceable to the logic that
produced them (see entity.NetworkRun.algorithmVersion on the Java side).
"""
import os

from dotenv import load_dotenv

load_dotenv()

DB_HOST = os.getenv("NETWORK_DB_HOST", "localhost")
DB_PORT = int(os.getenv("NETWORK_DB_PORT", "3306"))
DB_NAME = os.getenv("NETWORK_DB_NAME", "sentinel")
DB_USER = os.getenv("NETWORK_DB_USER", "network_analysis")
DB_PASSWORD = os.getenv("NETWORK_DB_PASSWORD", "")

ALGORITHM_VERSION = "1.0.0"

# Signal weights - must sum to 1.0 (validated in scoring.py). Mirrors the
# weighting strategy agreed for the project: fraud exposure (personalized
# PageRank seeded from confirmed-fraud accounts) dominates, since it is the
# most directly evidence-backed signal.
SIGNAL_WEIGHTS = {
    "fraud_exposure": 0.40,
    "shared_payee_concentration": 0.20,
    "community_membership": 0.15,
    "relationship_growth": 0.15,
    "dense_cluster": 0.10,
}

# Default lookback window for graph construction - deliberately bounded (not
# "all history") so old relationships don't dominate the projection.
DEFAULT_LOOKBACK_DAYS = 30

# A community (Louvain) at or above this size is called out in the
# human-readable reason text as a "dense interconnected account group" -
# NOT a "circular transaction" claim, since the underlying graph is a
# bipartite account/payee projection, not literal account-to-account
# transfers (see graph_builder.py).
DENSE_COMMUNITY_MIN_SIZE = 5

# An account's final network_risk_score at or above this (0-100 scale) counts
# towards NetworkRun.accounts_flagged, purely for the operator-facing summary
# card - does NOT gate whether a row gets written (every analyzed account gets
# a row, flagged or not).
FLAGGED_SCORE_THRESHOLD = 60.0


# How often the scheduler loop (scheduler.py) wakes up to check for a
# pending "Run Analysis Now" request and/or its own next scheduled run.
POLL_INTERVAL_SECONDS = int(os.getenv("NETWORK_POLL_INTERVAL_SECONDS", "300"))
SCHEDULED_INTERVAL_MINUTES = int(os.getenv("NETWORK_SCHEDULED_INTERVAL_MINUTES", "60"))


def sqlalchemy_url() -> str:
    return f"mysql+pymysql://{DB_USER}:{DB_PASSWORD}@{DB_HOST}:{DB_PORT}/{DB_NAME}"
