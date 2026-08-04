/**
 * NetworkAccountDetail.jsx
 *
 * Evidence panel for the selected account: the Python job's human-readable
 * network_reason text, a signal breakdown (parsed from evidenceJson - see
 * entity.AccountNetworkScore), a score-over-time timeline (growing
 * influence across runs is often more suspicious than one high score), and
 * the local neighborhood graph.
 */
import NetworkGraphView from './NetworkGraphView';

export default function NetworkAccountDetail({ detail, graph, onSelectNode }) {
  if (!detail) {
    return <div className="loading-state">Select an account above to see its evidence and neighborhood.</div>;
  }

  const { latest, timeline } = detail;
  let evidence = {};
  try {
    evidence = latest.evidence ? JSON.parse(latest.evidence) : {};
  } catch {
    evidence = {};
  }

  const maxScore = Math.max(...timeline.map((t) => t.networkRiskScore), 1);

  return (
    <div className="network-detail-panel">
      <div>
        <div className="network-reason">{latest.networkReason || 'No evidence text recorded for this run.'}</div>

        <div className="network-signal-grid">
          <Signal label="PageRank Percentile" value={evidence.page_rank_percentile} />
          <Signal label="Shared Payees" value={evidence.shared_payees} />
          <Signal label="Community Size" value={evidence.community_size} />
          <Signal label="Dense Community" value={evidence.is_dense_community ? 'Yes' : 'No'} />
          <Signal label="Growth Percentile" value={evidence.growth_percentile} />
          <Signal label="Confirmed-Fraud Neighbors" value={evidence.confirmed_fraud_neighbor_count} />
        </div>

        <div className="network-signal-label">Network Risk Score Over Time (run-to-run)</div>
        <div className="network-timeline">
          {timeline.map((point) => (
            <div
              key={point.runId}
              className="network-timeline-bar"
              title={`Run ${point.runId}: ${point.networkRiskScore} (${point.computedAt})`}
              style={{ height: `${Math.max((point.networkRiskScore / maxScore) * 100, 4)}%` }}
            />
          ))}
        </div>
      </div>

      <div>
        <NetworkGraphView graph={graph} onSelectNode={onSelectNode} />
      </div>
    </div>
  );
}

function Signal({ label, value }) {
  return (
    <div className="network-signal">
      <span className="network-signal-label">{label}</span>
      <span className="network-signal-value">{value ?? '—'}</span>
    </div>
  );
}
