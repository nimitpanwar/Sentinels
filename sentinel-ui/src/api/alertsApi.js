const BASE = '/api/alerts';

export async function fetchAlerts() {
  const res = await fetch(BASE);
  if (!res.ok) throw new Error(`Failed to fetch alerts: ${res.status}`);
  return res.json();
}

export async function fetchAlert(id) {
  const res = await fetch(`${BASE}/${id}`);
  if (!res.ok) throw new Error(`Failed to fetch alert ${id}: ${res.status}`);
  return res.json();
}

export async function updateAlertStatus(id, status, resolutionNotes = '') {
  const res = await fetch(`${BASE}/${id}/status`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ status, resolutionNotes }),
  });
  if (!res.ok) {
    const text = await res.text();
    throw new Error(text || `Status update failed: ${res.status}`);
  }
  return res.json();
}

export async function fetchAlertEvaluations(id) {
  const res = await fetch(`${BASE}/${id}/evaluations`);
  if (!res.ok) throw new Error(`Failed to fetch evaluations for alert ${id}: ${res.status}`);
  return res.json();
}

export async function fetchCaseAlerts(caseId) {
  const res = await fetch(`/api/cases/${caseId}/alerts`);
  if (!res.ok) throw new Error(`Failed to fetch alerts for case ${caseId}: ${res.status}`);
  return res.json();
}

export async function fetchRecentAccountTransactions(accountId, limit = 10) {
  const res = await fetch(`/api/accounts/${accountId}/transactions?limit=${limit}`);
  if (!res.ok) throw new Error(`Failed to fetch transactions for account ${accountId}: ${res.status}`);
  return res.json();
}
