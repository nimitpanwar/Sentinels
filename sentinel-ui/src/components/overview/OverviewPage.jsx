import { useState, useEffect, useMemo, useCallback } from 'react';
import { fetchCases, fetchCaseStats } from '../../api/casesApi';
import '../transactions/transactions.css';
import './overview.css';

const DAYS = 14;

/** Builds the last `DAYS` date keys (YYYY-MM-DD), oldest first. */
function buildDayKeys() {
  const keys = [];
  const now = new Date();
  for (let i = DAYS - 1; i >= 0; i--) {
    const d = new Date(now);
    d.setDate(d.getDate() - i);
    keys.push(d.toISOString().slice(0, 10));
  }
  return keys;
}

function formatDayLabel(key) {
  const d = new Date(key);
  return d.toLocaleDateString(undefined, { month: 'short', day: 'numeric' });
}

/** Minutes (float, may be null/undefined) → "2h 15m" / "34m" for display. */
function formatMinutes(mins) {
  if (mins == null || Number.isNaN(mins)) return '—';
  const total = Math.round(mins);
  const h = Math.floor(total / 60);
  const m = total % 60;
  return h > 0 ? `${h}h ${m}m` : `${m}m`;
}

export default function OverviewPage() {
  const [cases, setCases] = useState([]);
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [caseList, caseStats] = await Promise.all([fetchCases(), fetchCaseStats()]);
      setCases(caseList);
      setStats(caseStats);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  // Real DB-backed operator resolution work — Case is the actual unit an
  // operator investigates and resolves (see CaseController). CLOSED means
  // genuinely resolved; DISMISSED means confirmed false positive — both
  // represent completed work, but the trend/KPI focus on true closures.
  const closedCases    = useMemo(() => cases.filter(c => c.status === 'CLOSED'), [cases]);
  const dismissedCases = useMemo(() => cases.filter(c => c.status === 'DISMISSED'), [cases]);
  const resolvedCount  = closedCases.length + dismissedCases.length;

  const totalCases     = stats?.totalCases ?? cases.length;
  const resolutionRate = totalCases > 0 ? Math.round((resolvedCount / totalCases) * 100) : 0;

  const series = useMemo(() => {
    const dayKeys = buildDayKeys();
    const counts = Object.fromEntries(dayKeys.map(k => [k, 0]));
    closedCases.forEach(c => {
      if (!c.closedAt) return;
      const key = new Date(c.closedAt).toISOString().slice(0, 10);
      if (key in counts) counts[key] += 1;
    });
    return dayKeys.map(k => ({ key: k, label: formatDayLabel(k), count: counts[k] }));
  }, [closedCases]);

  const chart = useMemo(() => {
    const width = 720, height = 240, padX = 32, padTop = 20, padBottom = 34;
    const max = Math.max(1, ...series.map(p => p.count));
    const innerW = width - padX * 2;
    const innerH = height - padTop - padBottom;
    const stepX = series.length > 1 ? innerW / (series.length - 1) : 0;
    const points = series.map((p, i) => ({
      x: padX + i * stepX,
      y: padTop + innerH - (p.count / max) * innerH,
      ...p,
    }));
    const linePath = points.map((p, i) => `${i === 0 ? 'M' : 'L'} ${p.x.toFixed(1)} ${p.y.toFixed(1)}`).join(' ');
    const areaPath = `${linePath} L ${points[points.length - 1]?.x ?? padX} ${padTop + innerH} L ${padX} ${padTop + innerH} Z`;
    const gridLines = [0, 0.25, 0.5, 0.75, 1].map(f => padTop + innerH * f);
    return { width, height, points, linePath, areaPath, gridLines, max };
  }, [series]);

  return (
    <div className="page-overview">
      <div className="page-header">
        <h1 className="page-title">Overview</h1>
        <span className="row-count">Operator progress — closed cases, last {DAYS} days</span>
      </div>

      {error && <div className="error-banner">{error}</div>}

      {loading ? (
        <div className="loading-state">Loading overview…</div>
      ) : (
        <>
          <div className="overview-cards">
            <div className="overview-card">
              <div className="overview-card__label">Closed Cases</div>
              <div className="overview-card__value">{closedCases.length}</div>
            </div>
            <div className="overview-card overview-card--amber">
              <div className="overview-card__label">Resolution Rate</div>
              <div className="overview-card__value">{resolutionRate}%</div>
            </div>
            <div className="overview-card overview-card--amber">
              <div className="overview-card__label">Avg. Time to Close</div>
              <div className="overview-card__value">{formatMinutes(stats?.avgMinutesToClose)}</div>
            </div>
            <div className="overview-card overview-card--amber">
              <div className="overview-card__label">Avg. Time to Acknowledge</div>
              <div className="overview-card__value">{formatMinutes(stats?.avgMinutesToAcknowledge)}</div>
            </div>
          </div>

          <div className="overview-chart-panel">
            <h2 className="overview-chart-title">Closed Cases / Day</h2>
            <svg viewBox={`0 0 ${chart.width} ${chart.height}`} className="overview-chart-svg" preserveAspectRatio="none">
              {chart.gridLines.map((y, i) => (
                <line key={i} x1="32" x2={chart.width - 32} y1={y} y2={y} className="overview-gridline" />
              ))}
              <path d={chart.areaPath} className="overview-area" />
              <path d={chart.linePath} className="overview-line" />
              {chart.points.map((p, i) => (
                <circle key={i} cx={p.x} cy={p.y} r="3.5" className="overview-dot">
                  <title>{`${p.label}: ${p.count} closed`}</title>
                </circle>
              ))}
            </svg>
            <div className="overview-chart-labels">
              {series.map((p, i) => (
                (i % 2 === 0 || i === series.length - 1) && (
                  <span key={p.key} className="overview-chart-label">{p.label}</span>
                )
              ))}
            </div>
          </div>
        </>
      )}
    </div>
  );
}
