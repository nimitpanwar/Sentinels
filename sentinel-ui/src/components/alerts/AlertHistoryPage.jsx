import { useEffect, useMemo, useState } from 'react';
import { fetchDismissedAlerts, fetchAlertHistory } from '../../api/alertsApi';
import AlertFilterBar from './AlertFilterBar';
import { EMPTY_ALERT_FILTERS, filterAlerts } from '../../utils/alertFilterUtils';
import './alerts.css';
import '../transactions/transactions.css';

function fmtDate(iso) {
  if (!iso) return '—';
  return new Date(iso).toLocaleString('en-GB', {
    day: '2-digit', month: 'short', year: 'numeric',
    hour: '2-digit', minute: '2-digit', second: '2-digit',
  });
}

function fmtAmount(amount) {
  if (amount == null) return '—';
  return new Intl.NumberFormat('en-US', {
    style: 'currency', currency: 'USD', minimumFractionDigits: 2,
  }).format(amount);
}

function customerDisplayName(account) {
  if (!account) return '—';
  const first = account.customer?.firstName?.trim() ?? '';
  const last = account.customer?.lastName?.trim() ?? '';
  const full = `${first} ${last}`.trim();
  if (full) return full;
  if (account.customerName && account.customerName.trim()) return account.customerName.trim();
  return '—';
}

export default function AlertHistoryPage() {
  const [alerts, setAlerts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [selectedAlert, setSelectedAlert] = useState(null);
  const [history, setHistory] = useState(null);
  const [historyLoading, setHistoryLoading] = useState(false);
  const [historyError, setHistoryError] = useState(null);
  const [filters, setFilters] = useState(EMPTY_ALERT_FILTERS);

  useEffect(() => {
    let mounted = true;
    const load = async () => {
      setLoading(true);
      setError(null);
      try {
        const data = await fetchDismissedAlerts();
        if (mounted) {
          setAlerts(data);
        }
      } catch (err) {
        if (mounted) {
          setError(err.message);
        }
      } finally {
        if (mounted) {
          setLoading(false);
        }
      }
    };
    load();
    return () => {
      mounted = false;
    };
  }, []);

  const filtered = useMemo(() => filterAlerts(alerts, filters), [alerts, filters]);

  function handleFiltersChange(next) {
    setFilters(next);
  }

  async function openHistory(alert) {
    setSelectedAlert(alert);
    setHistory(null);
    setHistoryError(null);
    setHistoryLoading(true);
    try {
      const payload = await fetchAlertHistory(alert.alertId);
      setHistory(payload);
    } catch (err) {
      setHistoryError(err.message);
    } finally {
      setHistoryLoading(false);
    }
  }

  return (
    <div className="page-alerts">
      <div className="page-header">
        <h1 className="page-title">Alert History</h1>
        {!loading && !error && (
          <span className="row-count">
            Showing {filtered.length} dismissed alert{filtered.length !== 1 ? 's' : ''}
          </span>
        )}
      </div>

      <p className="alert-history-subtitle">
        Read-only archive of dismissed alerts with full lifecycle timeline and resolution notes.
      </p>

      <AlertFilterBar filters={filters} onFiltersChange={handleFiltersChange} />

      {loading && <div className="alerts-loading">Loading alert history…</div>}
      {error && <div className="alerts-empty">Error: {error}</div>}

      {!loading && !error && (
        <div className="alerts-table-wrap">
          <table className="alerts-table">
            <thead>
              <tr>
                <th>Alert ID</th>
                <th>Severity</th>
                <th>Status</th>
                <th>Customer</th>
                <th>Account No.</th>
                <th>Payee</th>
                <th className="num">Amount</th>
                <th>Dismissed At</th>
                <th>Notes</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map(alert => {
                const tx = alert.transaction ?? {};
                const acct = tx.account ?? {};
                const payee = tx.payee ?? {};
                return (
                  <tr key={alert.alertId} onClick={() => openHistory(alert)}>
                    <td className="mono">{alert.alertId}</td>
                    <td>{alert.severity ?? '—'}</td>
                    <td>{alert.status ?? '—'}</td>
                    <td>{customerDisplayName(acct)}</td>
                    <td className="mono">{acct.accountNumber ?? '—'}</td>
                    <td>{payee.payeeName ?? '—'}</td>
                    <td className="num">{fmtAmount(tx.amount)}</td>
                    <td>{fmtDate(alert.closedAt)}</td>
                    <td className="alert-history-note-cell">{alert.resolutionNotes || '—'}</td>
                  </tr>
                );
              })}
            </tbody>
          </table>
          {!filtered.length && <div className="alerts-empty">No dismissed alerts found.</div>}
        </div>
      )}

      {selectedAlert && (
        <div className="dismiss-overlay" onClick={() => setSelectedAlert(null)}>
          <div className="history-modal" onClick={e => e.stopPropagation()}>
            <div className="sibling-popup__header">
              <div className="sibling-popup__title-row">
                <span className="sibling-popup__id">Alert #{selectedAlert.alertId} Lifecycle</span>
              </div>
              <button
                className="filter-modal-close"
                onClick={() => setSelectedAlert(null)}
                aria-label="Close"
              >
                <svg viewBox="0 0 20 20" fill="currentColor" width="18" height="18" aria-hidden="true">
                  <path fillRule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clipRule="evenodd" />
                </svg>
              </button>
            </div>

            {historyLoading && <div className="alerts-loading">Loading lifecycle timeline…</div>}
            {historyError && <div className="alerts-empty">Error: {historyError}</div>}

            {!historyLoading && !historyError && history && (
              <>
                <div className="history-summary-grid">
                  <div><strong>Severity:</strong> {history.severity ?? '—'}</div>
                  <div><strong>Status:</strong> {history.status ?? '—'}</div>
                  <div><strong>Dismissed At:</strong> {fmtDate(history.dismissedAt)}</div>
                  <div><strong>Reason Code:</strong> {history.resolutionReasonCode ?? '—'}</div>
                </div>
                <div className="history-notes-block">
                  <strong>Resolution Notes:</strong>
                  <p>{history.resolutionNotes || '—'}</p>
                </div>
                <div className="history-timeline">
                  {(history.events || []).map((event, idx) => (
                    <div key={`${event.eventType}-${idx}`} className="history-timeline-item">
                      <div className="history-timeline-dot" />
                      <div className="history-timeline-content">
                        <div className="history-timeline-title">{event.title}</div>
                        <div className="history-timeline-time">{fmtDate(event.at)}</div>
                        {event.details && <div className="history-timeline-details">{event.details}</div>}
                      </div>
                    </div>
                  ))}
                  {(!history.events || history.events.length === 0) && (
                    <div className="alerts-empty">No recorded lifecycle events for this alert.</div>
                  )}
                </div>
              </>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
