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
