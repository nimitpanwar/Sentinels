/**
 * overviewUtils.js
 *
 * Shared helpers for the Overview page. Everything here is computed
 * client-side from the same `alerts` array already loaded in App.jsx
 * (GET /api/alerts returns the full, unpaginated list) - no extra
 * backend endpoint is needed for these aggregate stats.
 */

// Any status other than OPEN counts as "handled" - the operator has taken
// some action on it (acknowledged, is investigating, dismissed, or closed).
// IN_REVIEW / ESCALATED are legacy CaseStatus values kept for old rows.
const HANDLED_STATUSES = new Set([
  'ACKNOWLEDGED', 'INVESTIGATING', 'DISMISSED', 'CLOSED', 'IN_REVIEW', 'ESCALATED',
]);

export function isHandled(alert) {
  return HANDLED_STATUSES.has(alert.status);
}

function toDateKey(isoString) {
  return isoString ? isoString.slice(0, 10) : null;
}

/**
 * Daily count of alerts handled over the last `days` days (default 14).
 * "Handled that day" = the day the alert was closed, or acknowledged if it
 * hasn't been closed yet. Days with no activity are included as 0 so the
 * line graph always shows a continuous trend.
 */
export function computeHandledDailySeries(alerts, days = 14) {
  const counts = new Map();
  const today = new Date();

  for (let i = days - 1; i >= 0; i--) {
    const d = new Date(today);
    d.setDate(d.getDate() - i);
    counts.set(d.toISOString().slice(0, 10), 0);
  }

  for (const alert of alerts) {
    if (!isHandled(alert)) continue;
    const dateKey = toDateKey(alert.closedAt || alert.acknowledgedAt);
    if (dateKey && counts.has(dateKey)) {
      counts.set(dateKey, counts.get(dateKey) + 1);
    }
  }

  return Array.from(counts.entries()).map(([date, count]) => ({ date, count }));
}

export function computeOverviewStats(alerts) {
  const handled = alerts.filter(isHandled);
  return {
    totalAlerts: alerts.length,
    handledCount: handled.length,
    openCount: alerts.filter(a => a.status === 'OPEN').length,
    closedCount: alerts.filter(a => a.status === 'CLOSED').length,
    dismissedCount: alerts.filter(a => a.status === 'DISMISSED').length,
    dailySeries: computeHandledDailySeries(alerts),
  };
}
