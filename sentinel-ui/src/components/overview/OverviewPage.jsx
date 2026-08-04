import { useEffect, useMemo } from 'react';
import { computeOverviewStats } from '../../utils/overviewUtils';
import OverviewLineChart from './OverviewLineChart';
import './overview.css';

export default function OverviewPage({ alerts, loading, error, onMount }) {
  useEffect(() => {
    onMount();
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const stats = useMemo(() => computeOverviewStats(alerts), [alerts]);

  return (
    <div className="page-overview">
      <div className="page-header">
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
        </>
      )}
    </div>
  );
}
