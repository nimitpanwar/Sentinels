const BASE = '/api/cases';

/** Full list of investigation cases (the operator's actual unit of work). */
export async function fetchCases() {
  const res = await fetch(BASE);
  if (!res.ok) throw new Error(`Failed to fetch cases: ${res.status}`);
  return res.json();
}

/** Aggregate stats: count by status + avg minutes to acknowledge/close (see CaseController#stats). */
export async function fetchCaseStats() {
  const res = await fetch(`${BASE}/stats`);
  if (!res.ok) throw new Error(`Failed to fetch case stats: ${res.status}`);
  return res.json();
}
