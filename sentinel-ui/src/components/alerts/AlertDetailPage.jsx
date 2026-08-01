import { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { fetchAlert, updateAlertStatus } from '../../api/alertsApi';
import './alerts.css';
import '../transactions/transactions.css';

/* ── Helpers ─────────────────────────────────────────────────── */
function severityBadge(severity) {
  if (!severity) return null;
  return <span className={`badge badge--${severity.toLowerCase()}`}>{severity}</span>;
}

function statusBadge(status) {
  if (!status) return null;
  const clsMap = {
    OPEN: 'open', ACKNOWLEDGED: 'acknowledged', INVESTIGATING: 'investigating',
    DISMISSED: 'dismissed', IN_REVIEW: 'in-review', ESCALATED: 'escalated', CLOSED: 'closed',
  };
  return (
    <span className={`badge badge--${clsMap[status] ?? status.toLowerCase()}`}>
      {status.replace(/_/g, ' ')}
    </span>
  );
}

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

/* ── Component ───────────────────────────────────────────────── */
export default function AlertDetailPage({ updateAlert }) {
  const { id } = useParams();
  const navigate = useNavigate();
  const [alert, setAlert]           = useState(null);
  const [loading, setLoading]       = useState(true);
  const [error, setError]           = useState(null);
  const [actionError, setActionError] = useState(null);
  const [working, setWorking]       = useState(false);

  // Dismiss modal state
  const [showDismiss, setShowDismiss] = useState(false);
  const [dismissNotes, setDismissNotes] = useState('');
  const notesRef = useRef(null);

  useEffect(() => {
    fetchAlert(id)
      .then(data => {
        if (data.status === 'OPEN') {
          return updateAlertStatus(id, 'ACKNOWLEDGED')
            .then(updated => { if (updateAlert) updateAlert(updated); return updated; })
            .catch(() => data);
        }
        return data;
      })
      .then(setAlert)
      .catch(err => setError(err.message))
      .finally(() => setLoading(false));
  }, [id]);

  useEffect(() => {
    if (showDismiss && notesRef.current) notesRef.current.focus();
  }, [showDismiss]);

  async function handleDismiss() {
    setWorking(true);
    setActionError(null);
    try {
      const updated = await updateAlertStatus(id, 'DISMISSED', dismissNotes);
      setAlert(updated);
      if (updateAlert) updateAlert(updated);  // patch App-level cache
      setShowDismiss(false);
      setDismissNotes('');
    } catch (err) {
      setActionError(err.message);
    } finally {
      setWorking(false);
    }
  }

  const isTerminal = alert && (alert.status === 'DISMISSED' || alert.status === 'CLOSED');
  const tx   = alert?.transaction ?? {};
  const acct = tx.account ?? {};
  const payee = tx.payee ?? {};

  return (
    <div className="page-alert-detail">
      {/* Back link */}
      <button className="alert-detail-back" onClick={() => navigate('/alerts')}>
        ← Back to Alerts
      </button>

      {loading && <div className="alerts-loading">Loading alert…</div>}
      {error   && <div className="alerts-empty">Error: {error}</div>}

      {!loading && !error && alert && (
        <>
          {/* ── Header ── */}
          <div className="alert-detail-header">
            <h1 className="alert-detail-title">Alert #{alert.alertId}</h1>
            {severityBadge(alert.severity)}
            {statusBadge(alert.status)}
          </div>

          {/* ── Cards row ── */}
          <div className="alert-detail-cards">

            {/* Alert info */}
            <div className="alert-detail-card">
              <h2 className="alert-detail-card-title">Alert Info</h2>
              <dl className="alert-detail-dl">
                <dt>Risk Score</dt>  <dd className="num">{alert.riskScore ?? '—'}</dd>
                <dt>Created</dt>     <dd>{fmtDate(alert.createdAt)}</dd>
                <dt>Acknowledged</dt><dd>{fmtDate(alert.acknowledgedAt)}</dd>
                {isTerminal && <><dt>Closed</dt><dd>{fmtDate(alert.closedAt)}</dd></>}
                {alert.resolutionNotes && (
                  <><dt>Notes</dt><dd className="alert-detail-notes">{alert.resolutionNotes}</dd></>
                )}
              </dl>
            </div>

            {/* Transaction info */}
            <div className="alert-detail-card">
              <h2 className="alert-detail-card-title">Transaction</h2>
              <dl className="alert-detail-dl">
                <dt>TX ID</dt>       <dd className="mono">{tx.transactionId ?? '—'}</dd>
                <dt>Amount</dt>      <dd className="num">{fmtAmount(tx.amount)}</dd>
                <dt>Type</dt>        <dd>{tx.type ?? '—'}</dd>
                <dt>Timestamp</dt>   <dd>{fmtDate(tx.transactionTimestamp)}</dd>
                <dt>Account No.</dt> <dd className="mono">{acct.accountNumber ?? '—'}</dd>
                <dt>Customer</dt>    <dd>{acct.customerName ?? '—'}</dd>
                <dt>Payee</dt>       <dd>{payee.payeeName ?? '—'}</dd>
                <dt>Description</dt> <dd>{tx.description ?? '—'}</dd>
                <dt>Location</dt>    <dd>{tx.location ?? '—'}</dd>
              </dl>
            </div>
          </div>

          {/* ── Actions ── */}
          {!isTerminal && (
            <div className="alert-detail-actions">
              {actionError && <p className="alert-action-error">{actionError}</p>}
              <button
                className="btn btn--danger"
                onClick={() => setShowDismiss(true)}
                disabled={working}
              >
                Dismiss Alert
              </button>
              <button className="btn btn--secondary" disabled title="Coming soon">
                Investigate
              </button>
            </div>
          )}

          {/* ── Dismiss modal ── */}
          {showDismiss && (
            <div className="dismiss-overlay" onClick={() => setShowDismiss(false)}>
              <div className="dismiss-modal" onClick={e => e.stopPropagation()}>
                <h2 className="dismiss-modal__title">Dismiss Alert #{alert.alertId}?</h2>
                <p className="dismiss-modal__sub">
                  This marks the alert as a false positive. This action cannot be undone.
                </p>
                <label className="dismiss-modal__label" htmlFor="dismiss-notes">
                  Resolution notes <span className="muted">(optional)</span>
                </label>
                <textarea
                  id="dismiss-notes"
                  ref={notesRef}
                  className="dismiss-modal__textarea"
                  rows={3}
                  value={dismissNotes}
                  onChange={e => setDismissNotes(e.target.value)}
                  placeholder="e.g. Confirmed legitimate transaction with customer"
                />
                {actionError && <p className="alert-action-error">{actionError}</p>}
                <div className="dismiss-modal__buttons">
                  <button className="btn btn--ghost" onClick={() => setShowDismiss(false)} disabled={working}>
                    Cancel
                  </button>
                  <button className="btn btn--danger" onClick={handleDismiss} disabled={working}>
                    {working ? 'Dismissing…' : 'Confirm Dismiss'}
                  </button>
                </div>
              </div>
            </div>
          )}
        </>
      )}
    </div>
  );
}

