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

export async function updateAlertStatus(id, status, resolutionNotes = '', updateScope = 'ALERT') {
  const res = await fetch(`${BASE}/${id}/status`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ status, resolutionNotes, updateScope }),
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

export async function fetchInvestigationThread(id) {
  const res = await fetch(`${BASE}/${id}/investigation/thread`);
  if (!res.ok) throw new Error(`Failed to fetch investigation thread for alert ${id}: ${res.status}`);
  return res.json();
}

export async function sendInvestigationMessage(id, subject, body) {
  const res = await fetch(`${BASE}/${id}/investigation/send`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ subject, body }),
  });
  if (!res.ok) {
    const text = await res.text();
    throw new Error(text || `Failed to send investigation message: ${res.status}`);
  }
  return res.json();
}

export async function startInvestigation(id) {
  const res = await fetch(`${BASE}/${id}/investigation/start`, {
    method: 'PATCH',
  });
  if (!res.ok) {
    const text = await res.text();
    throw new Error(text || `Failed to start investigation: ${res.status}`);
  }
  return res.json();
}

export async function applyInvestigationAction(id, action, analystNotes = '', subject = '', body = '') {
  const res = await fetch(`${BASE}/${id}/investigation/action`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ action, analystNotes, subject, body }),
  });
  if (!res.ok) {
    const text = await res.text();
    throw new Error(text || `Failed to apply investigation action: ${res.status}`);
  }
  return res.json();
}

export async function fetchInvestigationResponseContext(token) {
  const res = await fetch(`/api/investigation/respond/${token}`);
  if (!res.ok) {
    const text = await res.text();
    throw new Error(text || `Failed to fetch response context: ${res.status}`);
  }
  return res.json();
}

export async function submitInvestigationResponse(token, payload) {
  const res = await fetch(`/api/investigation/respond/${token}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });
  if (!res.ok) {
    const text = await res.text();
    throw new Error(text || `Failed to submit response: ${res.status}`);
  }
  return res.json();
}
