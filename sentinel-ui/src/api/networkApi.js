/**
 * networkApi.js
 *
 * Thin wrapper around the backend's read-only network-analysis endpoints
 * (see controller/NetworkController.java). All scoring/graph algorithms run
 * in the separate Python batch job - this API only ever reads what that job
 * (or the on-demand shared-payee query) has already produced.
 */

/** One page of the latest completed run's ranked accounts. */
export async function fetchNetworkScores({ page = 0, size = 25, minScore } = {}) {
  const params = new URLSearchParams({ page, size });
  if (minScore !== undefined && minScore !== null && minScore !== '') {
    params.set('minScore', minScore);
  }
  const res = await fetch(`/api/network/scores?${params}`);
  if (!res.ok) throw new Error(`Failed to load network scores (HTTP ${res.status})`);
  return res.json();
}

/** Latest score + evidence + score-over-time timeline for one account. */
export async function fetchAccountNetworkDetail(accountId) {
  const res = await fetch(`/api/network/accounts/${accountId}`);
  if (res.status === 404) return null;
  if (!res.ok) throw new Error(`Failed to load account network detail (HTTP ${res.status})`);
  return res.json();
}

/** Small shared-payee neighborhood subgraph for one account. */
export async function fetchAccountGraph(accountId) {
  const res = await fetch(`/api/network/accounts/${accountId}/graph`);
  if (res.status === 404) return null;
  if (!res.ok) throw new Error(`Failed to load account graph (HTTP ${res.status})`);
  return res.json();
}

/** Run history (freshness/staleness of the batch job). */
export async function fetchNetworkRuns({ page = 0, size = 10 } = {}) {
  const params = new URLSearchParams({ page, size });
  const res = await fetch(`/api/network/runs?${params}`);
  if (!res.ok) throw new Error(`Failed to load network runs (HTTP ${res.status})`);
  return res.json();
}

/**
 * Operator-triggered "Run Analysis Now" - directly launches the Python job on
 * the backend and waits for it to finish (see NetworkController.requestRun).
 * This call takes a few seconds while the analysis actually runs.
 */
export async function requestNetworkRun(lookbackDays = 30) {
  const res = await fetch('/api/network/analysis/run', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ lookbackDays }),
  });
  const body = await res.json().catch(() => null);
  if (!res.ok) {
    throw new Error(body?.message || `Failed to run analysis (HTTP ${res.status})`);
  }
  return body;
}
