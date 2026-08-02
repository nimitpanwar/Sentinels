/**
 * NetworkSummaryCards.jsx
 *
 * Top-of-page summary cards: accounts analyzed, accounts flagged, clusters
 * detected, and the last run's freshness - mirrors the Alerts Dashboard
 * summary-card pattern described in transaction_monitoring.md Appendix C.
 */
export default function NetworkSummaryCards({ latestRun, flaggedCount, communityCount }) {
  const lastRunLabel = latestRun?.completedAt
    ? timeAgo(latestRun.completedAt)
    : 'Never run yet';

  return (
    <div className="network-summary-cards">
      <div className="summary-card">
        <div className="summary-card-value">{latestRun?.accountsAnalyzed ?? '—'}</div>
        <div className="summary-card-label">Accounts Analyzed</div>
      </div>
      <div className="summary-card">
        <div className="summary-card-value">{flaggedCount ?? '—'}</div>
        <div className="summary-card-label">Accounts Flagged</div>
      </div>
      <div className="summary-card">
        <div className="summary-card-value">{communityCount ?? '—'}</div>
        <div className="summary-card-label">Clusters Detected</div>
      </div>
      <div className="summary-card">
        <div className="summary-card-value">{lastRunLabel}</div>
        <div className="summary-card-label">Last Analysis Run</div>
      </div>
    </div>
  );
}

function timeAgo(isoString) {
  const then = new Date(isoString.endsWith('Z') ? isoString : isoString + 'Z');
  const diffMs = Date.now() - then.getTime();
  const minutes = Math.round(diffMs / 60000);
  if (minutes < 1) return 'Just now';
  if (minutes < 60) return `${minutes}m ago`;
  const hours = Math.round(minutes / 60);
  if (hours < 24) return `${hours}h ago`;
  const days = Math.round(hours / 24);
  return `${days}d ago`;
}
