import { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  fetchAlert, updateAlertStatus,
  fetchAlertEvaluations, fetchCaseAlerts, fetchRecentAccountTransactions,
  fetchInvestigationThread, sendInvestigationMessage, startInvestigation, applyInvestigationAction,
  fetchInvestigationProfile, updateInvestigationProfile, submitHighRiskSelfApproval,
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

function customerDisplayName(account) {
  if (!account) return '—';
  const first = account.customer?.firstName?.trim() ?? '';
  const last = account.customer?.lastName?.trim() ?? '';
  const full = `${first} ${last}`.trim();
  if (full) return full;
  if (account.customerName && account.customerName.trim()) return account.customerName.trim();
  return '—';
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
  const [updateScope, setUpdateScope] = useState('ALERT');
  const [showScopeConfirm, setShowScopeConfirm] = useState(false);
  const [pendingAction, setPendingAction] = useState(null);
  const [actionSuccess, setActionSuccess] = useState(null);
  const [activeTab, setActiveTab]     = useState('overview');
  const [thread, setThread]           = useState([]);
  const [threadLoading, setThreadLoading] = useState(false);
  const [profile, setProfile] = useState(null);
  const [profileLoading, setProfileLoading] = useState(false);
  const [analystNote, setAnalystNote] = useState('');
  const [checklistComplete, setChecklistComplete] = useState(false);
  const [highRiskJustification, setHighRiskJustification] = useState('');
  const [highConfirmOne, setHighConfirmOne] = useState(false);
  const [highConfirmTwo, setHighConfirmTwo] = useState(false);
  const [highSkipCooldown, setHighSkipCooldown] = useState(false);
  const [composeSubject, setComposeSubject] = useState('Sentinel Alert Verification Required - Please Confirm Recent Activity');
  const [composeBody, setComposeBody] = useState('Hello,\n\nWe detected unusual activity on your account. Please review and respond using the secure link provided below:\n\n{{response_link}}\n\nIf this transaction was not authorized by you, please mention that in your response.\n\nRegards,\nSentinel Investigations Team');

  // Dismiss modal state
  const [showDismiss, setShowDismiss] = useState(false);
  const [dismissNotes, setDismissNotes] = useState('');
  const notesRef = useRef(null);
  const previousResponseCountRef = useRef(0);

  // Sibling alert popup
  const [selectedSibling, setSelectedSibling] = useState(null);

  useEffect(() => {
    setProfile(null);
    setAnalystNote('');
    setChecklistComplete(false);
    setHighRiskJustification('');
    setHighConfirmOne(false);
    setHighConfirmTwo(false);
    setHighSkipCooldown(false);

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
        setAnalystNote(alertData.case?.investigationAnalystNote ?? '');
        setChecklistComplete(Boolean(alertData.case?.investigationChecklistComplete));
        setHighRiskJustification(alertData.case?.highRiskJustification ?? '');
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
        const p4 = fetchInvestigationThread(id).then(setThread).catch(() => {});
        const p5 = fetchInvestigationProfile(id).then(setProfile).catch(() => {});
        return Promise.all([p1, p2, p3, p4, p5]);
      })
      .catch(err => setError(err.message))
      .finally(() => setLoading(false));
  }, [id]);

  useEffect(() => {
    previousResponseCountRef.current = thread.filter(item => item.responseStatus === 'RESPONDED').length;
  }, [id]);

  useEffect(() => {
    if (activeTab !== 'investigation') {
      return;
    }

    const refreshThread = async () => {
      try {
        const latest = await fetchInvestigationThread(id);
        const currentCount = latest.filter(item => item.responseStatus === 'RESPONDED').length;
        if (currentCount > previousResponseCountRef.current) {
          setActionSuccess('New customer response received.');
        }
        previousResponseCountRef.current = currentCount;
        setThread(latest);
      } catch {
        // Silent polling failure: manual actions already surface actionable errors.
      }
    };

    refreshThread();
    const intervalId = setInterval(refreshThread, 10000);
    return () => clearInterval(intervalId);
  }, [activeTab, id]);

  useEffect(() => {
    if (activeTab !== 'investigation') {
      return;
    }
    let mounted = true;
    const loadProfile = async () => {
      setProfileLoading(true);
      try {
        const next = await fetchInvestigationProfile(id);
        if (mounted) {
          setProfile(next);
        }
      } catch {
        // Keep page usable if profile fetch fails temporarily.
      } finally {
        if (mounted) {
          setProfileLoading(false);
        }
      }
    };
    loadProfile();
    return () => { mounted = false; };
  }, [activeTab, id]);

  useEffect(() => {
    if (showDismiss && notesRef.current) notesRef.current.focus();
  }, [showDismiss]);

  async function handleDismiss() {
    if (updateScope === 'CASE') {
      setPendingAction('DISMISS');
      setShowScopeConfirm(true);
      return;
    }
    await runDismiss();
  }

  async function runDismiss(scope = updateScope) {
    setWorking(true);
    setActionError(null);
    setActionSuccess(null);
    try {
      const updated = await updateAlertStatus(id, 'DISMISSED', dismissNotes, scope);
      setAlert(updated);
      if (updateAlert) updateAlert(updated);  // patch App-level cache
      setShowDismiss(false);
      setDismissNotes('');
      if (scope === 'CASE') {
        setActionSuccess(`Case-level update applied to ${caseAlerts.length || 1} alert(s).`);
      } else {
        setActionSuccess('Alert updated successfully.');
      }
    } catch (err) {
      setActionError(err.message);
    } finally {
      setWorking(false);
    }
  }

  async function handleSendInvestigation() {
    if (!composeSubject.trim() || !composeBody.trim()) {
      setActionError('Subject and message body are required');
      return;
    }
    setWorking(true);
    setActionError(null);
    try {
      const result = await sendInvestigationMessage(id, composeSubject, composeBody);
      if (result.alert) {
        setAlert(result.alert);
        if (updateAlert) updateAlert(result.alert);
      }
      setThreadLoading(true);
      const latestThread = await fetchInvestigationThread(id);
      setThread(latestThread);
      setActiveTab('investigation');
    } catch (err) {
      setActionError(err.message);
    } finally {
      setThreadLoading(false);
      setWorking(false);
    }
  }

  async function handleAnalystAction(action) {
    setWorking(true);
    setActionError(null);
    setActionSuccess(null);
    try {
      if (profile?.blockedReasons?.length && action === 'DISMISS') {
        throw new Error(profile.blockedReasons[0]);
      }
      const result = await applyInvestigationAction(id, action, dismissNotes, composeSubject, composeBody);
      if (result.alert) {
        setAlert(result.alert);
        if (updateAlert) updateAlert(result.alert);
      }
      const latestThread = await fetchInvestigationThread(id);
      setThread(latestThread);
      const latestProfile = await fetchInvestigationProfile(id);
      setProfile(latestProfile);
      if (action === 'DISMISS') {
        setActionSuccess('Alert dismissed from investigation workflow.');
      } else if (action === 'FLAG') {
        setActionSuccess('Alert escalated for deeper review.');
      } else {
        setActionSuccess('Follow-up outreach sent with a new response link.');
      }
    } catch (err) {
      setActionError(err.message);
    } finally {
      setWorking(false);
    }
  }

  async function handleSaveProfile() {
    setWorking(true);
    setActionError(null);
    try {
      const updatedProfile = await updateInvestigationProfile(id, {
        analystNote,
        checklistComplete,
      });
      setProfile(updatedProfile);
      setActionSuccess('Investigation checklist progress saved.');
    } catch (err) {
      setActionError(err.message);
    } finally {
      setWorking(false);
    }
  }

  async function handleHighRiskSelfApproval() {
    setWorking(true);
    setActionError(null);
    try {
      const updatedProfile = await submitHighRiskSelfApproval(id, {
        justification: highRiskJustification,
        confirmOne: highConfirmOne,
        confirmTwo: highConfirmTwo,
        skipCooldown: highSkipCooldown,
      });
      setProfile(updatedProfile);
      setActionSuccess('High-risk self-approval recorded. Cooldown started.');
    } catch (err) {
      setActionError(err.message);
    } finally {
      setWorking(false);
    }
  }

  async function handleStartInvestigation() {
    if (updateScope === 'CASE') {
      setPendingAction('INVESTIGATE');
      setShowScopeConfirm(true);
      return;
    }
    await runStartInvestigation();
  }

  async function runStartInvestigation(scope = updateScope) {
    setWorking(true);
    setActionError(null);
    setActionSuccess(null);
    try {
      let updated;
      if (scope === 'CASE') {
        updated = await updateAlertStatus(id, 'INVESTIGATING', '', 'CASE');
      } else {
        updated = await startInvestigation(id);
      }
      setAlert(updated);
      if (updateAlert) updateAlert(updated);
      setActiveTab('investigation');
      if (scope === 'CASE') {
        setActionSuccess(`Case-level update applied to ${caseAlerts.length || 1} alert(s).`);
      } else {
        setActionSuccess('Alert moved to investigating.');
      }
    } catch (err) {
      setActionError(err.message);
    } finally {
      setWorking(false);
    }
  }

  async function confirmScopeAction() {
    setShowScopeConfirm(false);
    if (pendingAction === 'INVESTIGATE') {
      await runStartInvestigation('CASE');
    } else if (pendingAction === 'DISMISS') {
      await runDismiss('CASE');
    }
    setPendingAction(null);
  }

  async function openInvestigationTab() {
    const current = alert?.status;
    // If already investigating/terminal, just switch tabs with no API call.
    if (!current || current === 'INVESTIGATING' || current === 'ESCALATED' || current === 'CLOSED' || current === 'DISMISSED') {
      setActiveTab('investigation');
      return;
    }
    await handleStartInvestigation();
  }

  const isTerminal = alert && (alert.status === 'DISMISSED' || alert.status === 'CLOSED');
  // Keep tabs visible once analyst has entered investigation mode, even if
  // backend status is still propagating and temporarily reports ACKNOWLEDGED.
  const showInvestigationTabs =
    alert?.status === 'INVESTIGATING' ||
    alert?.status === 'ESCALATED' ||
    activeTab === 'investigation';
  const tx   = alert?.transaction ?? {};
  const acct = tx.account ?? {};
  const payee = tx.payee ?? {};
  const responseCount = thread.filter(item => item.responseStatus === 'RESPONDED').length;
  const blockedReasons = profile?.blockedReasons ?? [];
  const finalActionBlocked = blockedReasons.length > 0;
  const isHighSeverity = (profile?.severity ?? alert?.case?.severity) === 'HIGH';

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

          {showInvestigationTabs && (
            <div className="investigation-tabs">
              <button
                className={`investigation-tab ${activeTab === 'overview' ? 'investigation-tab--active' : ''}`}
                onClick={() => setActiveTab('overview')}
              >
                Overview
              </button>
              <button
                className={`investigation-tab ${activeTab === 'investigation' ? 'investigation-tab--active' : ''}`}
                onClick={openInvestigationTab}
              >
                Investigation
              </button>
            </div>
          )}

          {showInvestigationTabs && activeTab === 'investigation' && (
            <div className="alert-detail-tab-content">
            <div className="investigation-panel">
              {actionError && <p className="alert-action-error">{actionError}</p>}
              {isTerminal && (
                <div className="investigation-terminal-note">
                  This alert is in a terminal state. Outreach is locked, but historical thread entries are available below.
                </div>
              )}
              <div className="investigation-compose">
                <h2 className="alert-detail-card-title">Customer Outreach Email</h2>
                <label className="dismiss-modal__label" htmlFor="investigation-subject">Subject</label>
                <input
                  id="investigation-subject"
                  className="investigation-input"
                  value={composeSubject}
                  onChange={e => setComposeSubject(e.target.value)}
                  placeholder="Email subject"
                />
                <label className="dismiss-modal__label" htmlFor="investigation-body">Message</label>
                <textarea
                  id="investigation-body"
                  className="dismiss-modal__textarea"
                  rows={8}
                  value={composeBody}
                  onChange={e => setComposeBody(e.target.value)}
                />
                <p className="investigation-hint">
                  Use {'{{response_link}}'} placeholder in the message body. It will be replaced by a secure response URL in the next phase.
                </p>
                <div className="dismiss-modal__buttons">
                  <button className="btn btn--secondary" onClick={handleSendInvestigation} disabled={working || isTerminal}>
                    {working ? 'Sending…' : 'Send Investigation Email'}
                  </button>
                </div>
              </div>
              <div className="investigation-profile-card investigation-profile-card--tile">
                <h2 className="alert-detail-card-title">Investigation Checklist</h2>
                {profileLoading && <div className="alerts-loading">Loading severity profile…</div>}
                {profile && (
                  <>
                    <div className="investigation-profile-head">
                      <span className="badge badge--acknowledged">Severity Profile: {profile.severity || 'LOW'}</span>
                      <span className="investigation-profile-progress">
                        {profile.completedSteps?.length || 0}/{profile.requiredSteps?.length || 0} required steps done
                      </span>
                    </div>
                    {profile.requiredSteps?.length > 0 && (
                      <ul className="investigation-required-list">
                        {profile.requiredSteps.map(step => {
                          const done = (profile.completedSteps || []).includes(step);
                          return (
                            <li key={step} className={`investigation-required-item ${done ? 'is-done' : 'is-missing'}`}>
                              <span>{done ? 'Done' : 'Pending'}</span>
                              <span>{step.replaceAll('_', ' ')}</span>
                            </li>
                          );
                        })}
                      </ul>
                    )}

                    <label className="dismiss-modal__label" htmlFor="investigation-analyst-note">Analyst Note</label>
                    <textarea
                      id="investigation-analyst-note"
                      className="dismiss-modal__textarea"
                      rows={3}
                      value={analystNote}
                      onChange={e => setAnalystNote(e.target.value)}
                      placeholder="Add your investigation note"
                    />

                    <label className="investigation-checkbox-row">
                      <input
                        type="checkbox"
                        checked={checklistComplete}
                        onChange={e => setChecklistComplete(e.target.checked)}
                      />
                      <span>Checklist complete</span>
                    </label>

                    <div className="dismiss-modal__buttons">
                      <button className="btn btn--ghost" onClick={handleSaveProfile} disabled={working}>
                        Save Progress
                      </button>
                    </div>
                  </>
                )}
              </div>

              <div className="investigation-profile-card investigation-profile-card--tile">
                <h2 className="alert-detail-card-title">
                  {isHighSeverity ? 'High-Risk Self-Approval' : 'Final Action Gates'}
                </h2>

                {isHighSeverity ? (
                  <div className="high-risk-panel">
                    <p className="high-risk-copy">
                      HIGH severity final actions need self-approval with a short cooldown.
                    </p>
                    <label className="dismiss-modal__label" htmlFor="high-risk-justification">Final Justification</label>
                    <textarea
                      id="high-risk-justification"
                      className="dismiss-modal__textarea"
                      rows={4}
                      value={highRiskJustification}
                      onChange={e => setHighRiskJustification(e.target.value)}
                      placeholder="Explain why final action is safe"
                    />
                    <label className="investigation-checkbox-row">
                      <input
                        type="checkbox"
                        checked={highConfirmOne}
                        onChange={e => setHighConfirmOne(e.target.checked)}
                      />
                      <span>I understand this is a high-risk final action.</span>
                    </label>
                    <label className="investigation-checkbox-row">
                      <input
                        type="checkbox"
                        checked={highConfirmTwo}
                        onChange={e => setHighConfirmTwo(e.target.checked)}
                      />
                      <span>I confirm I reviewed all available evidence.</span>
                    </label>
                    {Boolean(profile?.allowSkipCooldown) && (
                      <label className="investigation-checkbox-row">
                        <input
                          type="checkbox"
                          checked={highSkipCooldown}
                          onChange={e => setHighSkipCooldown(e.target.checked)}
                        />
                        <span>Skip cooldown (demo mode)</span>
                      </label>
                    )}
                    {(profile?.cooldownRemainingSeconds || 0) > 0 && (
                      <p className="high-risk-cooldown">
                        Cooldown remaining: {Math.ceil(profile.cooldownRemainingSeconds / 60)} minute(s)
                      </p>
                    )}
                    <div className="dismiss-modal__buttons">
                      <button className="btn btn--secondary" onClick={handleHighRiskSelfApproval} disabled={working}>
                        Record High-Risk Self-Approval
                      </button>
                    </div>
                  </div>
                ) : (
                  <p className="investigation-hint">
                    Complete required checklist steps for this severity before terminal actions.
                  </p>
                )}

                {blockedReasons.length > 0 && (
                  <div className="investigation-blocked-box">
                    <div className="investigation-blocked-title">Final actions are blocked</div>
                    <ul>
                      {blockedReasons.map(reason => <li key={reason}>{reason}</li>)}
                    </ul>
                  </div>
                )}
              </div>
            </div>

            <div className="investigation-thread">
              <h2 className="alert-detail-card-title">Investigation Thread</h2>
                <div className="investigation-thread-summary">
                  <span className="badge badge--acknowledged">Responses received: {responseCount}</span>
                  <button
                    className="btn btn--ghost btn--tiny"
                    type="button"
                    onClick={async () => {
                      setThreadLoading(true);
                      try {
                        const latestThread = await fetchInvestigationThread(id);
                        previousResponseCountRef.current = latestThread.filter(item => item.responseStatus === 'RESPONDED').length;
                        setThread(latestThread);
                      } finally {
                        setThreadLoading(false);
                      }
                    }}
                    disabled={working || threadLoading}
                  >
                    {threadLoading ? 'Refreshing…' : 'Refresh'}
                  </button>
                </div>
                {threadLoading && <div className="alerts-loading">Refreshing thread…</div>}
                {!threadLoading && thread.length === 0 && (
                  <div className="alerts-empty">No outreach sent for this alert yet.</div>
                )}
                {!threadLoading && thread.length > 0 && (
                  <ul className="investigation-thread-list">
                    {thread.map(item => (
                      <li key={item.messageId} className="investigation-thread-item">
                        <div className="investigation-thread-item__top">
                          <span className="mono">Message #{item.messageId}</span>
                          <span className={`badge badge--${item.deliveryStatus === 'SENT' ? 'open' : 'dismissed'}`}>
                            {item.deliveryStatus}
                          </span>
                        </div>
                        <div className="investigation-thread-meta">
                          <span>To: {item.deliveredRecipientEmail || '—'}</span>
                          <span>Sent: {fmtDate(item.sentAt)}</span>
                          <span>Token Expires: {fmtDate(item.responseTokenExpiresAt)}</span>
                        </div>
                        <div className="investigation-thread-subject">{item.subject}</div>
                        <pre className="investigation-thread-body">{item.bodySnapshot}</pre>
                        {item.responseStatus === 'RESPONDED' && (
                          <div className="investigation-response-block">
                            <div className="investigation-response-title">Customer Response</div>
                            <div className="investigation-thread-meta">
                              <span>Submitted: {fmtDate(item.respondedAt)}</span>
                              <span>Name: {item.respondentName || '—'}</span>
                              <span>Email: {item.respondentEmail || '—'}</span>
                            </div>
                            <dl className="investigation-response-grid">
                              <dt>Recognized</dt>
                              <dd>{item.recognizedTransaction == null ? '—' : item.recognizedTransaction ? 'Yes' : 'No'}</dd>
                              <dt>Authorized</dt>
                              <dd>{item.authorizedTransaction == null ? '—' : item.authorizedTransaction ? 'Yes' : 'No'}</dd>
                            </dl>
                            <pre className="investigation-thread-body">{item.responseExplanation || '—'}</pre>
                          </div>
                        )}
                      </li>
                    ))}
                  </ul>
                )}

                {!isTerminal && (
                  <div className="investigation-analyst-actions">
                    <button className="btn btn--danger" onClick={() => handleAnalystAction('DISMISS')} disabled={working || finalActionBlocked}>
                      Dismiss
                    </button>
                    <button className="btn btn--secondary" onClick={() => handleAnalystAction('FLAG')} disabled={working}>
                      Flag / Escalate
                    </button>
                    <button className="btn btn--ghost" onClick={() => handleAnalystAction('REQUEST_MORE_INFO')} disabled={working}>
                      Request More Info
                    </button>
                  </div>
                )}
            </div>
            </div>
          )}

          {(activeTab === 'overview' || (!showInvestigationTabs && activeTab !== 'investigation')) && (
            <div className="alert-detail-tab-content">

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
                <dt>Customer</dt>    <dd>{customerDisplayName(acct)}</dd>
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
                        <td style={{ color: '#6b7280', fontSize: '0.78rem' }}>{e.reason ?? '—'}</td>
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
              {actionSuccess && <p className="alert-action-success">{actionSuccess}</p>}
              <div className="alert-scope-control">
                <span className="alert-scope-label">Update scope</span>
                <div className="alert-scope-segment">
                  <button
                    className={`scope-chip ${updateScope === 'ALERT' ? 'scope-chip--active' : ''}`}
                    onClick={() => setUpdateScope('ALERT')}
                    disabled={working}
                    type="button"
                  >
                    This alert
                  </button>
                  <button
                    className={`scope-chip ${updateScope === 'CASE' ? 'scope-chip--active' : ''}`}
                    onClick={() => setUpdateScope('CASE')}
                    disabled={working || !alert?.case?.caseId}
                    type="button"
                    title={alert?.case?.caseId ? '' : 'Case-level update not available'}
                  >
                    Entire case
                  </button>
                </div>
                {updateScope === 'CASE' && (
                  <p className="alert-scope-hint">
                    This action will update {caseAlerts.length || 1} alert(s) in Case #{alert?.case?.caseId ?? '—'}.
                  </p>
                )}
              </div>
              <button
                className="btn btn--danger"
                onClick={() => setShowDismiss(true)}
                disabled={working}
              >
                Dismiss Alert
              </button>
              <button
                className="btn btn--secondary"
                onClick={openInvestigationTab}
                disabled={working}
              >
                Investigate
              </button>
            </div>
          )}

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

          {showScopeConfirm && (
            <div className="dismiss-overlay" onClick={() => setShowScopeConfirm(false)}>
              <div className="dismiss-modal" onClick={e => e.stopPropagation()}>
                <h2 className="dismiss-modal__title">Apply update to entire case?</h2>
                <p className="dismiss-modal__sub">
                  You are about to apply this status update to {caseAlerts.length || 1} alert(s) in Case #{alert?.case?.caseId ?? '—'}.
                </p>
                <div className="dismiss-modal__buttons">
                  <button className="btn btn--ghost" onClick={() => setShowScopeConfirm(false)} disabled={working}>
                    Cancel
                  </button>
                  <button className="btn btn--secondary" onClick={confirmScopeAction} disabled={working}>
                    Confirm Case Update
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
                <dt>Customer</dt>    <dd>{customerDisplayName(sacct)}</dd>
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

