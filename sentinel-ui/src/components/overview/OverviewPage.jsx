import { useEffect, useMemo } from 'react';
import OverviewLineChart from './OverviewLineChart';
import OverviewBarChart from './OverviewBarChart';
import OverviewHeatmap from './OverviewHeatmap';
import { buildOverviewChartSeries, summarizeOverview, buildRiskHistogram, buildHourHeatmap } from '../../utils/overviewUtils';
import './overview.css';

export default function OverviewPage({ alerts, loading, error, onMount }) {
  useEffect(() => {
    onMount();
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const summary = useMemo(() => summarizeOverview(alerts), [alerts]);
  const series = useMemo(() => buildOverviewChartSeries(alerts), [alerts]);
  const histogram = useMemo(() => buildRiskHistogram(alerts), [alerts]);
  const heatmap = useMemo(() => buildHourHeatmap(alerts), [alerts]);

  return (
    <div className="page-overview">
      <div className="page-header">
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
              <article key={card.label} className="overview-card" style={{ '--card-accent': card.color }}>
                <div className="overview-card__label">{card.label}</div>
                <div className="overview-card__value" style={{ color: card.color }}>{card.value}</div>
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
              <OverviewHeatmap cells={heatmap} />
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
              <OverviewBarChart bars={summary.severityBreakdown} />
            </article>

            <article className="overview-panel">
              <div className="overview-panel__eyebrow">Status Mix</div>
              <h2 className="overview-panel__title">Workflow balance</h2>
              <OverviewBarChart bars={summary.statusBreakdown} />
            </article>

            <article className="overview-panel">
              <div className="overview-panel__eyebrow">Risk Score Distribution</div>
              <h2 className="overview-panel__title">Score buckets</h2>
              <OverviewBarChart bars={histogram} />
            </article>
          </section>
        </>
      )}
    </div>
  );
}
