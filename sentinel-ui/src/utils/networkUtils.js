/**
 * networkUtils.js
 *
 * Shared helpers for the Network Insights page components.
 */
export function riskColor(score) {
  if (score === null || score === undefined) return '#9ca3af';
  if (score >= 75) return '#dc2626';
  if (score >= 60) return '#f97316';
  if (score >= 40) return '#f59e0b';
  return '#10b981';
}
