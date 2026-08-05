import { useEffect, useMemo } from 'react';
<<<<<<< HEAD
import { computeOverviewStats } from '../../utils/overviewUtils';
import OverviewLineChart from './OverviewLineChart';
=======
import OverviewLineChart from './OverviewLineChart';
import { buildOverviewChartSeries, summarizeOverview } from '../../utils/overviewUtils';
>>>>>>> master
import './overview.css';

export default function OverviewPage({ alerts, loading, error, onMount }) {
  useEffect(() => {
    onMount();
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

<<<<<<< HEAD
  const stats = useMemo(() => computeOverviewStats(alerts), [alerts]);
=======
  const summary = useMemo(() => summarizeOverview(alerts), [alerts]);
  const series = useMemo(() => buildOverviewChartSeries(alerts), [alerts]);
>>>>>>> master

  return (
    <div className="page-overview">
      <div className="page-header">
<<<<<<< HEAD
        <h1 className="page-title">Overview</h1>
        <span className="overview-subtitle">Operator activity &amp; daily progress</span>
      </div>

      {loading && <div className="overview-loading">Loading overview…</div>}
      {error && <div className="overview-empty">Error: {error}</div>}

      {!loading && !error && (
        <>
          <div className="overview-cards">
            <div className="overview-card">
              <div className="overview-card__label">Alerts Handled</div>
              <div className="overview-card__value overview-card__value--success">{stats.handledCount}</div>
            </div>
            <div className="overview-card">
              <div className="overview-card__label">Open</div>
              <div className="overview-card__value overview-card__value--warning">{stats.openCount}</div>
            </div>
            <div className="overview-card">
              <div className="overview-card__label">Closed</div>
              <div className="overview-card__value">{stats.closedCount}</div>
            </div>
            <div className="overview-card">
              <div className="overview-card__label">Dismissed</div>
              <div className="overview-card__value">{stats.dismissedCount}</div>
            </div>
          </div>

          <div className="overview-chart-panel">
            <div className="overview-chart-panel__title">Alerts Handled — Last 14 Days</div>
            <OverviewLineChart data={stats.dailySeries} />
          </div>
=======
        <div>
          <div className="overview-eyebrow">Operational Dashboard</div>
          <h1 className="page-title">Overview</h1>
        </div>
        {!loading && !error && <span className="row-count">{summary.totalAlerts} alerts in current workspace snapshot</span>}
      </div>

      {loading && <div className="loading-state">Loading overview metrics…</div>}
      {error && <div className="error-banner">Error: {error}</div>}

      {!loading && !error && (
        <>
          <section className="overview-hero">
            <div className="overview-hero__copy">
              <div className="overview-hero__title">Command Center snapshot</div>
              <p>
                Monitor current alert pressure, severity balance, and recent throughput from the same alert payload already driving the existing workflow pages.
              </p>
            </div>
            <div className="overview-hero__meta">
              <span className="overview-pill">High severity: {summary.highSeverity}</span>
              <span className="overview-pill">Escalated: {summary.escalated}</span>
              <span className="overview-pill">Today: {summary.todayCount}</span>
            </div>
          </section>

          <section className="overview-grid overview-grid--cards">
            {summary.cards.map((card) => (
              <article key={card.label} className="overview-card">
                <div className="overview-card__label">{card.label}</div>
                <div className="overview-card__value">{card.value}</div>
                <div className="overview-card__hint">{card.hint}</div>
              </article>
            ))}
          </section>

          <section className="overview-grid overview-grid--primary">
            <article className="overview-panel">
              <div className="overview-panel__head">
                <div>
                  <div className="overview-panel__eyebrow">Alert Velocity</div>
                  <h2 className="overview-panel__title">Recent creation trend</h2>
                </div>
              </div>
              <OverviewLineChart points={series} />
            </article>

            <article className="overview-panel">
              <div className="overview-panel__eyebrow">Analyst Brief</div>
              <h2 className="overview-panel__title">Current posture</h2>
              <ul className="overview-brief-list">
                {summary.brief.map((item) => <li key={item}>{item}</li>)}
              </ul>
            </article>
          </section>

          <section className="overview-grid overview-grid--secondary">
            <article className="overview-panel">
              <div className="overview-panel__eyebrow">Severity Mix</div>
              <h2 className="overview-panel__title">Current distribution</h2>
              <div className="overview-breakdown">
                {summary.severityBreakdown.map((item) => (
                  <div key={item.label} className="overview-breakdown__row">
                    <span>{item.label}</span>
                    <span className="mono">{item.value}</span>
                  </div>
                ))}
              </div>
            </article>

            <article className="overview-panel">
              <div className="overview-panel__eyebrow">Status Mix</div>
              <h2 className="overview-panel__title">Workflow balance</h2>
              <div className="overview-breakdown">
                {summary.statusBreakdown.map((item) => (
                  <div key={item.label} className="overview-breakdown__row">
                    <span>{item.label}</span>
                    <span className="mono">{item.value}</span>
                  </div>
                ))}
              </div>
            </article>
          </section>
>>>>>>> master
        </>
      )}
    </div>
  );
<<<<<<< HEAD
}
=======
}
>>>>>>> master
