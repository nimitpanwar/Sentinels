import { useState, useCallback, useRef } from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import NavBar from './components/NavBar';
import TransactionsPage from './components/transactions/TransactionsPage';
import AlertsPage from './components/alerts/AlertsPage';
import AlertDetailPage from './components/alerts/AlertDetailPage';
import AlertHistoryPage from './components/alerts/AlertHistoryPage';
import NetworkPage from './components/network/NetworkPage';
import OverviewPage from './components/overview/OverviewPage';
import CustomerResponsePage from './components/investigation/CustomerResponsePage';
import { fetchAlerts } from './api/alertsApi';
import './App.css';

const STALE_MS = 30_000;

export default function App() {
  const [alerts, setAlerts]   = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError]     = useState(null);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const lastFetchedAt         = useRef(null);

  const loadAlerts = useCallback(async ({ background = false } = {}) => {
    if (!background) setLoading(true);
    setError(null);
    try {
      const data = await fetchAlerts();
      setAlerts(data);
      lastFetchedAt.current = Date.now();
    } catch (err) {
      setError(err.message);
    } finally {
      if (!background) setLoading(false);
    }
  }, []);

  const ensureLoaded = useCallback(() => {
    const isStale = !lastFetchedAt.current || Date.now() - lastFetchedAt.current > STALE_MS;
    if (alerts.length === 0) {
      loadAlerts();
    } else if (isStale) {
      loadAlerts({ background: true });
    }
  }, [alerts.length, loadAlerts]);

  const updateAlert = useCallback((updated) => {
    setAlerts(prev => prev.map(a => a.alertId === updated.alertId ? updated : a));
  }, []);

  const isCustomerResponseRoute = window.location.pathname.startsWith('/investigation/respond/');

  if (isCustomerResponseRoute) {
    return (
      <Routes>
        <Route path="/investigation/respond/:token" element={<CustomerResponsePage />} />
        <Route path="*" element={<Navigate to="/investigation/respond/invalid" replace />} />
      </Routes>
    );
  }

  return (
    <div className={`app-shell${drawerOpen ? ' app-shell--drawer-open' : ''}`}>
      <NavBar drawerOpen={drawerOpen} setDrawerOpen={setDrawerOpen} />
      <div className="app-content">
        <Routes>
          <Route path="/overview"
            element={
              <OverviewPage
                alerts={alerts}
                loading={loading}
                error={error}
                onMount={ensureLoaded}
              />
            }
          />
          <Route path="/transactions" element={<TransactionsPage />} />
          <Route path="/alerts"
            element={
              <AlertsPage
                alerts={alerts}
                loading={loading}
                error={error}
                onMount={ensureLoaded}
              />
            }
          />
          <Route path="/alert-history" element={<AlertHistoryPage />} />
          <Route path="/network" element={<NetworkPage />} />
          <Route path="/alerts/:id"
            element={<AlertDetailPage updateAlert={updateAlert} refreshAlerts={loadAlerts} />}
          />
          <Route path="/investigation/respond/:token" element={<CustomerResponsePage />} />
          <Route path="*" element={<Navigate to="/overview" replace />} />
        </Routes>
      </div>
    </div>
  );
}