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
      { label: 'Total Alerts', value: totalAlerts, hint: 'Current fetched alert inventory', color: '#7c83fd' },
      { label: 'Average Risk', value: averageRisk, hint: 'Average risk score across current snapshot', color: '#facc15' },
      { label: 'Investigating', value: investigating, hint: 'Alerts currently in active review', color: '#fb923c' },
      { label: 'Opened Today', value: todayCount, hint: 'New alerts created during today', color: '#00ff88' },
    ],
    brief: [
      `${highSeverity} alert(s) are marked HIGH severity in the current dataset.`,
      `${escalated} alert(s) are already escalated while ${open} remain OPEN.`,
      totalAlerts === 0 ? 'No alert data is loaded yet.' : `Average network pressure equivalent from current risk values is ${averageRisk}.`,
    ],
    severityBreakdown: [
      { label: 'High', value: alerts.filter(alert => alert.severity === 'HIGH').length, color: '#f87171' },
      { label: 'Mid', value: alerts.filter(alert => alert.severity === 'MID').length, color: '#fb923c' },
      { label: 'Low', value: alerts.filter(alert => alert.severity === 'LOW').length, color: '#4ade80' },
    ],
    statusBreakdown: [
      { label: 'Open', value: open, color: '#7c83fd' },
      { label: 'Acknowledged', value: alerts.filter(alert => alert.status === 'ACKNOWLEDGED').length, color: '#facc15' },
      { label: 'Investigating', value: investigating, color: '#fb923c' },
      { label: 'Escalated', value: escalated, color: '#f87171' },
    ],
  };
}

export function buildRiskHistogram(alerts) {
  const buckets = [
    { label: '0–19',   min: 0,  max: 19,  value: 0, color: '#4ade80' },
    { label: '20–39',  min: 20, max: 39,  value: 0, color: '#a3e635' },
    { label: '40–59',  min: 40, max: 59,  value: 0, color: '#facc15' },
    { label: '60–79',  min: 60, max: 79,  value: 0, color: '#fb923c' },
    { label: '80–100', min: 80, max: 100, value: 0, color: '#f87171' },
  ];
  for (const alert of alerts) {
    const score = Number(alert.riskScore || 0);
    for (const bucket of buckets) {
      if (score >= bucket.min && score <= bucket.max) {
        bucket.value += 1;
        break;
      }
    }
  }
  return buckets;
}

export function buildHourHeatmap(alerts) {
  const hours = Array.from({ length: 24 }, (_, i) => ({ hour: i, value: 0 }));
  for (const alert of alerts) {
    if (!alert.createdAt) continue;
    const h = new Date(alert.createdAt).getHours();
    hours[h].value += 1;
  }
  return hours;
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
