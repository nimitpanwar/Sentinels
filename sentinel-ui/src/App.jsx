import { useState, useCallback, useRef } from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import NavBar from './components/NavBar';
import TransactionsPage from './components/transactions/TransactionsPage';
import AlertsPage from './components/alerts/AlertsPage';
import AlertDetailPage from './components/alerts/AlertDetailPage';
import OverviewPage from './components/overview/OverviewPage';
import ChatbotPage from './components/chatbot/ChatbotPage';
import { fetchAlerts } from './api/alertsApi';

const STALE_MS = 30_000;

export default function App() {
  const [alerts, setAlerts]   = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError]     = useState(null);
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

  return (
    <>
      <NavBar />
      <Routes>
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
        <Route path="/alerts/:id"
          element={<AlertDetailPage updateAlert={updateAlert} />}
        />
        <Route path="/overview" element={<OverviewPage />} />
        <Route path="/assistant" element={<ChatbotPage />} />
        <Route path="*" element={<Navigate to="/transactions" replace />} />
      </Routes>
    </>
  );
}