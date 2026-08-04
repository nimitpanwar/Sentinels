/**
 * NetworkScoreTable.jsx
 *
 * Ranked, sortable-by-score table of accounts from the latest completed
 * network-analysis run. Row click selects an account, driving the detail
 * panel + neighborhood graph below (see NetworkPage.jsx).
 */
import { riskColor } from '../../utils/networkUtils';

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
            <td className="num">{row.sharedPayeeCount ?? '—'}</td>
            <td className="num">{row.communitySize ?? '—'}</td>
            <td className="num">{row.growthScore ?? '—'}</td>
            <td className="muted">{(row.computedAt || '').replace('T', ' ').slice(0, 19)}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
