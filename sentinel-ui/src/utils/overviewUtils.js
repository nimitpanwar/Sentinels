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