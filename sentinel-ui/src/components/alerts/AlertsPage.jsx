import { useState, useEffect, useMemo } from 'react';
import AlertSummaryCards from './AlertSummaryCards';
import AlertStatusTabs from './AlertStatusTabs';
import AlertTable from './AlertTable';
import AlertFilterBar from './AlertFilterBar';
import { filterAlerts, EMPTY_ALERT_FILTERS } from '../../utils/alertFilterUtils';
import './alerts.css';
import '../transactions/transactions.css';

const PAGE_SIZE = 20;

export default function AlertsPage({ alerts, loading, error, onMount }) {
  const [activeTab, setActiveTab]       = useState('ALL');
  const [visibleCount, setVisibleCount] = useState(PAGE_SIZE);
  const [filters, setFilters]           = useState(EMPTY_ALERT_FILTERS);

  useEffect(() => {
    onMount();
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  function handleTabChange(tab) {
    setActiveTab(tab);
    setVisibleCount(PAGE_SIZE);
  }

  function handleFiltersChange(newFilters) {
    setFilters(newFilters);
    setVisibleCount(PAGE_SIZE);
  }

  // 1. Apply modal filters across the full list
  // 2. Apply tab filter on top
  const filteredAlerts = useMemo(() => {
    const afterFilters = filterAlerts(alerts, filters);
    if (activeTab === 'ALL') return afterFilters;
    return afterFilters.filter(a => a.status === activeTab);
  }, [alerts, filters, activeTab]);

  const visibleAlerts = filteredAlerts.slice(0, visibleCount);
  const hasMore = visibleCount < filteredAlerts.length;

  return (
    <div className="page-alerts">
      <div className="page-header">
        <h1 className="page-title">Alerts</h1>
        {!loading && !error && (
          <span className="row-count">
            Showing {visibleAlerts.length} of {filteredAlerts.length} alert{filteredAlerts.length !== 1 ? 's' : ''}
          </span>
        )}
      </div>

      {loading && <div className="alerts-loading">Loading alerts…</div>}
      {error   && <div className="alerts-empty">Error: {error}</div>}

      {!loading && !error && (
        <>
          <AlertSummaryCards alerts={alerts} />
          <AlertFilterBar filters={filters} onFiltersChange={handleFiltersChange} />
          <AlertStatusTabs activeTab={activeTab} onTabChange={handleTabChange} />
          <AlertTable alerts={visibleAlerts} />

          {hasMore && (
            <div className="alerts-show-more">
              <button
                className="btn-show-more"
                onClick={() => setVisibleCount(c => c + PAGE_SIZE)}
              >
                Show More
              </button>
            </div>
          )}
        </>
      )}
    </div>
  );
}
