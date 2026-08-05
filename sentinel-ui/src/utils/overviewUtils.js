<<<<<<< HEAD
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
=======
function isToday(value) {
  if (!value) return false;
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return false;
  return date.toDateString() === new Date().toDateString();
}

export function summarizeOverview(alerts) {
  const totalAlerts = alerts.length;
  const highSeverity = alerts.filter(alert => alert.severity === 'HIGH').length;
  const escalated = alerts.filter(alert => alert.status === 'ESCALATED').length;
  const investigating = alerts.filter(alert => alert.status === 'INVESTIGATING').length;
  const open = alerts.filter(alert => alert.status === 'OPEN').length;
  const todayCount = alerts.filter(alert => isToday(alert.createdAt)).length;
  const averageRisk = totalAlerts ? (alerts.reduce((sum, alert) => sum + Number(alert.riskScore || 0), 0) / totalAlerts).toFixed(1) : '0.0';

  return {
    totalAlerts,
    highSeverity,
    escalated,
    todayCount,
    cards: [
      { label: 'Total Alerts', value: totalAlerts, hint: 'Current fetched alert inventory' },
      { label: 'Average Risk', value: averageRisk, hint: 'Average risk score across current snapshot' },
      { label: 'Investigating', value: investigating, hint: 'Alerts currently in active review' },
      { label: 'Opened Today', value: todayCount, hint: 'New alerts created during today' },
    ],
    brief: [
      `${highSeverity} alert(s) are marked HIGH severity in the current dataset.`,
      `${escalated} alert(s) are already escalated while ${open} remain OPEN.`,
      totalAlerts === 0 ? 'No alert data is loaded yet.' : `Average network pressure equivalent from current risk values is ${averageRisk}.`,
    ],
    severityBreakdown: [
      { label: 'High', value: alerts.filter(alert => alert.severity === 'HIGH').length },
      { label: 'Mid', value: alerts.filter(alert => alert.severity === 'MID').length },
      { label: 'Low', value: alerts.filter(alert => alert.severity === 'LOW').length },
    ],
    statusBreakdown: [
      { label: 'Open', value: open },
      { label: 'Acknowledged', value: alerts.filter(alert => alert.status === 'ACKNOWLEDGED').length },
      { label: 'Investigating', value: investigating },
      { label: 'Escalated', value: escalated },
    ],
  };
}

export function buildOverviewChartSeries(alerts) {
  const buckets = new Map();
  const now = new Date();

  for (let offset = 6; offset >= 0; offset -= 1) {
    const date = new Date(now);
    date.setDate(now.getDate() - offset);
    const key = date.toISOString().slice(0, 10);
    buckets.set(key, {
      label: date.toLocaleDateString('en-GB', { day: '2-digit', month: 'short' }),
      value: 0,
    });
  }

  for (const alert of alerts) {
    if (!alert.createdAt) continue;
    const key = new Date(alert.createdAt).toISOString().slice(0, 10);
    if (buckets.has(key)) {
      buckets.get(key).value += 1;
    }
  }

  return Array.from(buckets.values());
}
>>>>>>> master
