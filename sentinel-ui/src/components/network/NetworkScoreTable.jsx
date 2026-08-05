/**
 * NetworkScoreTable.jsx
 *
 * Ranked, sortable-by-score table of accounts from the latest completed
 * network-analysis run. Row click selects an account, driving the detail
 * panel + neighborhood graph below (see NetworkPage.jsx).
 */
import { dominantSignalLabel, formatTimestamp, freshnessLabel, riskBucket, riskColor } from '../../utils/networkUtils';

export default function NetworkScoreTable({ rows, selectedAccountId, onSelect }) {
  if (rows.length === 0) {
    return <div className="loading-state">No network scores yet — run the analysis job to populate this table.</div>;
  }

  return (
    <table className="network-table">
      <thead>
        <tr>
          <th>Account</th>
          <th>Network Risk Score</th>
          <th>Risk Context</th>
          <th>Shared Payees</th>
          <th>Community Size</th>
          <th>Growth %ile</th>
          <th>Computed At</th>
        </tr>
      </thead>
      <tbody>
        {rows.map((row) => (
          <tr
            key={row.accountId}
            className={row.accountId === selectedAccountId ? 'is-selected' : ''}
            onClick={() => onSelect(row.accountId)}
          >
            <td className="mono">{row.accountNumber || `#${row.accountId}`}</td>
            <td>
              <span
                className="score-bar-track"
                title={`${row.networkRiskScore}`}
              >
                <span
                  className="score-bar-fill"
                  style={{
                    width: `${Math.min(row.networkRiskScore, 100)}%`,
                    background: riskColor(row.networkRiskScore),
                  }}
                />
              </span>
              <span className="num">{row.networkRiskScore}</span>
            </td>
            <td>
              <div className="network-table-context">
                <span className={`network-pill network-pill--${riskBucket(row.networkRiskScore).toLowerCase()}`}>
                  {riskBucket(row.networkRiskScore)}
                </span>
                <span className="network-context-sub">{dominantSignalLabel(row)}</span>
              </div>
            </td>
            <td className="num">{row.sharedPayeeCount ?? '—'}</td>
            <td className="num">{row.communitySize ?? '—'}</td>
            <td className="num">{row.growthScore ?? '—'}</td>
            <td>
              <div className="network-table-context">
                <span className="muted">{formatTimestamp(row.computedAt)}</span>
                <span className={`network-context-sub network-context-sub--${freshnessLabel(row.computedAt).toLowerCase()}`}>
                  {freshnessLabel(row.computedAt)}
                </span>
              </div>
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
