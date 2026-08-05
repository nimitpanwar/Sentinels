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
import { useMemo, useState } from 'react';
import {
  deriveRedFlags,
  dominantSignalLabel,
  formatTimestamp,
  freshnessLabel,
  parseEvidence,
  riskBucket,
  summarizeTopSignals,
  trendSummary,
} from '../../utils/networkUtils';

export default function NetworkAccountDetail({
  detail,
  graph,
  graphLoading,
  graphError,
  accountTrail,
  onSelectNode,
  onSelectTrailAccount,
}) {
  if (!detail) {
    return <div className="loading-state">Select an account above to see its evidence and neighborhood.</div>;
  }

  const { latest, timeline } = detail;
  const [showLabels, setShowLabels] = useState(true);
  const [minSharedPayees, setMinSharedPayees] = useState(1);
  const [neighborCap, setNeighborCap] = useState(30);

  const filteredGraph = useMemo(() => {
    if (!graph || !graph.nodes?.length) return graph;

    const center = graph.nodes.find((n) => n.isCenter) || graph.nodes[0];
    const edgeByNeighbor = new Map(
      (graph.edges || []).map((e) => [
        e.targetAccountId === center.accountId ? e.sourceAccountId : e.targetAccountId,
        e,
      ])
    );

    const eligible = graph.nodes
      .filter((n) => !n.isCenter)
      .filter((n) => (edgeByNeighbor.get(n.accountId)?.sharedPayeeCount ?? 0) >= minSharedPayees)
      .sort((a, b) => (edgeByNeighbor.get(b.accountId)?.sharedPayeeCount ?? 0) - (edgeByNeighbor.get(a.accountId)?.sharedPayeeCount ?? 0))
      .slice(0, neighborCap);

    const eligibleSet = new Set(eligible.map((n) => n.accountId));
    const edges = (graph.edges || []).filter((e) =>
      eligibleSet.has(e.sourceAccountId) || eligibleSet.has(e.targetAccountId)
    );

    return {
      ...graph,
      nodes: [center, ...eligible],
      edges,
    };
  }, [graph, minSharedPayees, neighborCap]);

  const evidence = parseEvidence(latest.evidence);
  const topSignals = summarizeTopSignals(evidence, latest);
  const redFlags = deriveRedFlags(evidence, latest);
  const trend = trendSummary(timeline);
  const freshness = freshnessLabel(latest.computedAt);

  const maxScore = Math.max(...timeline.map((t) => t.networkRiskScore), 1);

  return (
    <div className="network-detail-panel">
      <div>
        <div className="network-analyst-summary">
          <div className="network-summary-head">
            <div>
              <div className="network-summary-kicker">Analyst Summary</div>
              <div className="network-summary-title">
                {riskBucket(latest.networkRiskScore)} risk account with {dominantSignalLabel(latest, evidence).toLowerCase()} network pressure
              </div>
            </div>
            <div className="network-summary-metrics">
              <span className={`network-pill network-pill--${riskBucket(latest.networkRiskScore).toLowerCase()}`}>
                Score {latest.networkRiskScore}
              </span>
              <span className={`network-pill network-pill--neutral network-pill--${freshness.toLowerCase()}`}>
                {freshness}
              </span>
            </div>
          </div>

          <ul className="network-summary-bullets">
            {topSignals.map((item) => <li key={item}>{item}</li>)}
          </ul>

          {redFlags.length > 0 && (
            <div className="network-flag-row">
              {redFlags.map((flag) => (
                <span key={flag} className="network-flag-chip">{flag}</span>
              ))}
            </div>
          )}
        </div>

        <div className="network-reason">{latest.networkReason || 'No evidence text recorded for this run.'}</div>

        <div className="network-signal-grid">
          <Signal label="PageRank Percentile" value={evidence.page_rank_percentile} />
          <Signal label="Shared Payees" value={evidence.shared_payees} />
          <Signal label="Community Size" value={evidence.community_size} />
          <Signal label="Dense Community" value={evidence.is_dense_community ? 'Yes' : 'No'} />
          <Signal label="Growth Percentile" value={evidence.growth_percentile} />
          <Signal label="Confirmed-Fraud Neighbors" value={evidence.confirmed_fraud_neighbor_count} />
        </div>

        <div className="network-trend-summary">
          <div>
            <div className="network-signal-label">Network Risk Score Over Time (run-to-run)</div>
            <div className="network-trend-copy">{trend.summary}</div>
          </div>
          <div className="network-trend-meta">
            <span>Runs: {trend.runCount}</span>
            <span>Last updated: {formatTimestamp(latest.computedAt)}</span>
          </div>
        </div>
        <div className="network-timeline">
          {timeline.map((point) => (
            <div
              key={point.runId}
              className="network-timeline-bar"
              title={`Run ${point.runId}: ${point.networkRiskScore} (${formatTimestamp(point.computedAt)})`}
              style={{ height: `${Math.max((point.networkRiskScore / maxScore) * 100, 4)}%` }}
            />
          ))}
        </div>
        <div className="network-timeline-labels">
          {timeline.length > 0 ? timeline.map((point) => (
            <span key={`label-${point.runId}`} className="network-timeline-label">
              {point.networkRiskScore}
            </span>
          )) : <span className="muted">No run history yet.</span>}
        </div>
      </div>

      <div>
        {accountTrail?.length > 0 && (
          <div className="network-breadcrumb" aria-label="Account path">
            {accountTrail.map((accountId, idx) => (
              <button
                key={`${accountId}-${idx}`}
                type="button"
                className={`network-breadcrumb-chip ${idx === accountTrail.length - 1 ? 'is-active' : ''}`}
                onClick={() => onSelectTrailAccount?.(accountId)}
              >
                #{accountId}
              </button>
            ))}
          </div>
        )}

        <div className="network-graph-meta">Shared-payee neighborhood (projection), not direct transfer links.</div>

        <div className="network-graph-controls">
          <label className="network-control-checkbox">
            <input
              type="checkbox"
              checked={showLabels}
              onChange={(e) => setShowLabels(e.target.checked)}
            />
            Show labels
          </label>

          <label>
            Min shared payees
            <input
              type="number"
              min="1"
              max="20"
              value={minSharedPayees}
              onChange={(e) => setMinSharedPayees(Math.max(1, Number(e.target.value || 1)))}
            />
          </label>

          <label>
            Neighbor cap
            <select value={neighborCap} onChange={(e) => setNeighborCap(Number(e.target.value))}>
              <option value={10}>Top 10</option>
              <option value={20}>Top 20</option>
              <option value={30}>Top 30</option>
            </select>
          </label>
        </div>

        <div className="network-legend">
          <span className="network-legend-title">Risk Color</span>
          <span><i className="legend-dot legend-dot--high" /> High (&gt;=75)</span>
          <span><i className="legend-dot legend-dot--elevated" /> Elevated (60-74)</span>
          <span><i className="legend-dot legend-dot--guarded" /> Guarded (40-59)</span>
          <span><i className="legend-dot legend-dot--low" /> Low (&lt;40)</span>
        </div>

        {graphLoading && <div className="loading-state">Loading graph neighborhood…</div>}
        {!graphLoading && graphError && <div className="error-banner">Graph unavailable: {graphError}</div>}
        {!graphLoading && !graphError && (
          <NetworkGraphView graph={filteredGraph} onSelectNode={onSelectNode} showLabels={showLabels} />
        )}
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
