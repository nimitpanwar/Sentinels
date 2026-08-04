/**
 * networkUtils.js
 *
 * Shared helpers for the Network Insights page components.
 */
export function riskColor(score) {
  if (score === null || score === undefined) return '#8a8d91';
  if (score >= 75) return '#ff4c4c';
  if (score >= 60) return '#ff9900';
  if (score >= 40) return '#ffcc00';
  return '#00ff88';
}
