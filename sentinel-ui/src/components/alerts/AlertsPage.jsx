import { useState, useEffect, useMemo } from 'react';
import AlertSummaryCards from './AlertSummaryCards';
import AlertStatusTabs from './AlertStatusTabs';
import AlertTable from './AlertTable';
import './alerts.css';
import '../transactions/transactions.css';

export default function AlertsPage({ alerts, loading, error, onMount }) {
  const [activeTab, setActiveTab] = useState('ALL');

  useEffect(() => {
    onMount();
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const visibleAlerts = useMemo(() => {
    if (activeTab === 'ALL') return alerts;
    return alerts.filter(a => a.status === activeTab);
  }, [alerts, activeTab]);

  return (
    <div className="page-alerts">
      <div className="page-header">
        <h1 className="page-title">Alerts</h1>
        {!loading && !error && (
          <span className="row-count">{visibleAlerts.length} alert{visibleAlerts.length !== 1 ? 's' : ''}</span>
        )}
      </div>

      {loading && <div className="alerts-loading">Loading alerts…</div>}
      {error   && <div className="alerts-empty">Error: {error}</div>}

      {!loading && !error && (
        <>
          <AlertSummaryCards alerts={alerts} />
          <AlertStatusTabs activeTab={activeTab} onTabChange={setActiveTab} />
          <AlertTable alerts={visibleAlerts} />
        </>
      )}
    </div>
  );
}
