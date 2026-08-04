import { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  fetchAlert, updateAlertStatus,
  fetchAlertEvaluations, fetchCaseAlerts, fetchRecentAccountTransactions,
} from '../../api/alertsApi';
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
  const [alert, setAlert]             = useState(null);
  const [evaluations, setEvaluations] = useState([]);
  const [caseAlerts, setCaseAlerts]   = useState([]);
  const [recentTx, setRecentTx]       = useState([]);
  const [loading, setLoading]         = useState(true);
  const [error, setError]             = useState(null);
  const [actionError, setActionError] = useState(null);
  const [working, setWorking]         = useState(false);

  // Dismiss modal state
  const [showDismiss, setShowDismiss] = useState(false);
  const [dismissNotes, setDismissNotes] = useState('');
  const notesRef = useRef(null);

  // Sibling alert popup
  const [selectedSibling, setSelectedSibling] = useState(null);

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
      .then(alertData => {
        setAlert(alertData);
        const txId   = alertData.transaction?.transactionId;
        const acctId = alertData.transaction?.account?.accountId;
        const caseId = alertData.case?.caseId;

        // Parallel fetch of supplementary data
        const p1 = fetchAlertEvaluations(id).then(setEvaluations).catch(() => {});
        const p2 = acctId
          ? fetchRecentAccountTransactions(acctId, 10).then(setRecentTx).catch(() => {})
          : Promise.resolve();
        const p3 = caseId
          ? fetchCaseAlerts(caseId).then(setCaseAlerts).catch(() => {})
          : Promise.resolve();
        return Promise.all([p1, p2, p3]);
      })
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

          {/* ── Cards row 1: Alert Info + Transaction ── */}
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

            {/* ── Rule Evaluations — full width ── */}
            {evaluations.length > 0 && (
              <div className="alert-detail-card alert-detail-card--full">
                <h2 className="alert-detail-card-title">Rule Evaluations</h2>
                <table className="eval-table">
                  <thead>
                    <tr>
                      <th>Rule</th>
                      <th>Triggered</th>
                      <th className="num">Score</th>
                      <th>Reason</th>
                    </tr>
                  </thead>
                  <tbody>
                    {evaluations.map(e => (
                      <tr key={e.evaluationId} className={e.triggered ? 'eval-triggered' : ''}>
                        <td>{e.rule?.ruleName ?? e.rule?.ruleType ?? '—'}</td>
                        <td>{e.triggered
                          ? <span className="badge badge--high">Yes</span>
                          : <span className="badge badge--closed">No</span>}
                        </td>
                        <td className="num">{e.riskScore != null ? parseFloat(e.riskScore).toFixed(3) : '—'}</td>
                        <td style={{ color: '#ffffff', fontSize: '0.78rem' }}>{e.reason ?? '—'}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}

            {/* ── Case Summary ── */}
            {alert.case && (
              <div className="alert-detail-card">
                <h2 className="alert-detail-card-title">Case</h2>
                <dl className="alert-detail-dl" style={{ marginBottom: 12 }}>
                  <dt>Case ID</dt>    <dd className="mono">#{alert.case.caseId}</dd>
                  <dt>Severity</dt>   <dd>{severityBadge(alert.case.severity)}</dd>
                  <dt>Status</dt>     <dd>{statusBadge(alert.case.status)}</dd>
                  <dt>Risk Score</dt> <dd className="num">{alert.case.riskScore ?? '—'}</dd>
                </dl>
                {caseAlerts.length > 1 && (
                  <>
                    <div className="alert-detail-card-title" style={{ marginBottom: 6 }}>
                      Alerts in this case ({caseAlerts.length})
                    </div>
                    <ul className="case-alerts-list">
                      {caseAlerts.map(ca => (
                        <li
                          key={ca.alertId}
                          className={`case-alert-item ${ca.alertId === alert.alertId ? 'case-alert-item--current' : ''}`}
                          onClick={() => ca.alertId !== alert.alertId && setSelectedSibling(ca)}
                        >
                          <span className="case-alert-id">#{ca.alertId}</span>
                          {statusBadge(ca.status)}
                          {severityBadge(ca.severity)}
                          <span className="case-alert-date">{fmtDate(ca.createdAt)}</span>
                        </li>
                      ))}
                    </ul>
                  </>
                )}
              </div>
            )}

            {/* ── Recent Account Activity ── */}
            {recentTx.length > 0 && (
              <div className="alert-detail-card">
                <h2 className="alert-detail-card-title">Recent Account Activity</h2>
                <table className="recent-tx-table">
                  <thead>
                    <tr>
                      <th>TX ID</th>
                      <th className="num">Amount</th>
                      <th>Payee</th>
                      <th>Date</th>
                    </tr>
                  </thead>
                  <tbody>
                    {recentTx.map(t => (
                      <tr key={t.transactionId}
                          className={t.transactionId === tx.transactionId ? 'tx-current' : ''}>
                        <td className="mono">{t.transactionId}</td>
                        <td className="num">{fmtAmount(t.amount)}</td>
                        <td>{t.payee?.payeeName ?? '—'}</td>
                        <td>{fmtDate(t.transactionTimestamp)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}

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

      {/* ── Sibling alert popup ── */}
      {selectedSibling && (() => {
        const s    = selectedSibling;
        const stx  = s.transaction ?? {};
        const sacct = stx.account  ?? {};
        const spayee = stx.payee   ?? {};
        return (
          <div className="dismiss-overlay" onClick={() => setSelectedSibling(null)}>
            <div className="sibling-popup" onClick={e => e.stopPropagation()}>

              <div className="sibling-popup__header">
                <div className="sibling-popup__title-row">
                  <span className="sibling-popup__id">Alert #{s.alertId}</span>
                  {severityBadge(s.severity)}
                  {statusBadge(s.status)}
                </div>
                <button
                  className="filter-modal-close"
                  onClick={() => setSelectedSibling(null)}
                  aria-label="Close"
                >
                  <svg viewBox="0 0 20 20" fill="currentColor" width="18" height="18" aria-hidden="true">
                    <path fillRule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clipRule="evenodd" />
                  </svg>
                </button>
              </div>

              <dl className="alert-detail-dl sibling-popup__dl">
                <dt>Risk Score</dt>  <dd className="num">{s.riskScore ?? '—'}</dd>
                <dt>Amount</dt>      <dd className="num">{fmtAmount(stx.amount)}</dd>
                <dt>Payee</dt>       <dd>{spayee.payeeName ?? '—'}</dd>
                <dt>Account</dt>     <dd className="mono">{sacct.accountNumber ?? '—'}</dd>
                <dt>Customer</dt>    <dd>{sacct.customerName ?? '—'}</dd>
                <dt>Created</dt>     <dd>{fmtDate(s.createdAt)}</dd>
                <dt>Acknowledged</dt><dd>{fmtDate(s.acknowledgedAt)}</dd>
              </dl>

              <div className="sibling-popup__footer">
                <button className="btn btn--ghost" onClick={() => setSelectedSibling(null)}>
                  Close
                </button>
                <button
                  className="btn btn--secondary"
                  onClick={() => { setSelectedSibling(null); navigate(`/alerts/${s.alertId}`); }}
                >
                  View Full Alert →
                </button>
              </div>
            </div>
          </div>
        );
      })()}
    </div>
  );
}

