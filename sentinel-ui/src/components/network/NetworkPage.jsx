/**
 * NetworkPage.jsx
 *
 * Root page for Network Insights (see notes/RiskLogic.md "Graph Network
 * Risk Logic"). Scores come from the Python/NetworkX batch job. Clicking
 * "Run Analysis Now" directly launches that job on the backend and waits
 * for it to finish (see NetworkController.requestRun) - the request takes
 * a few seconds while the analysis actually runs, then the table/summary
 * cards below refresh automatically with the new results.
 *
 * Flow: summary cards -> ranked accounts table -> click a row -> evidence
 * panel + score timeline + small local neighborhood graph.
 */
import { useCallback, useEffect, useState } from 'react';
import {
  fetchAccountGraph,
  fetchAccountNetworkDetail,
  fetchNetworkRuns,
  fetchNetworkScores,
  requestNetworkRun,
} from '../../api/networkApi';
import NetworkSummaryCards from './NetworkSummaryCards';
import NetworkScoreTable from './NetworkScoreTable';
import NetworkAccountDetail from './NetworkAccountDetail';
import './network.css';

export default function NetworkPage() {
  const [scores, setScores] = useState([]);
  const [latestRun, setLatestRun] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [minScore, setMinScore] = useState('');
  const [lookbackDays, setLookbackDays] = useState(30);
  const [runRequesting, setRunRequesting] = useState(false);
  const [runMessage, setRunMessage] = useState(null);

  const [selectedAccountId, setSelectedAccountId] = useState(null);
  const [detail, setDetail] = useState(null);
  const [graph, setGraph] = useState(null);
  const [graphLoading, setGraphLoading] = useState(false);
  const [graphError, setGraphError] = useState(null);
  const [accountTrail, setAccountTrail] = useState([]);

  const loadScores = useCallback(() => {
    setLoading(true);
    setError(null);
    Promise.all([
      fetchNetworkScores({ page: 0, size: 50, minScore: minScore || undefined }),
      fetchNetworkRuns({ page: 0, size: 1 }),
    ])
      .then(([scorePage, runPage]) => {
        setScores(scorePage.content);
        setLatestRun(runPage.content[0] || null);
      })
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, [minScore]);

  useEffect(() => { loadScores(); }, [loadScores]);

  const selectAccount = useCallback(async (accountId, source = 'table') => {
    setSelectedAccountId(accountId);
    setDetail(null);
    setGraph(null);
    setGraphError(null);
    setGraphLoading(true);

    if (source === 'graph') {
      setAccountTrail((prev) => {
        const seenIdx = prev.indexOf(accountId);
        if (seenIdx >= 0) return prev.slice(0, seenIdx + 1);
        return [...prev.slice(-4), accountId];
      });
    } else {
      setAccountTrail([accountId]);
    }

    const [detailResult, graphResult] = await Promise.allSettled([
      fetchAccountNetworkDetail(accountId),
      fetchAccountGraph(accountId),
    ]);

    if (detailResult.status === 'fulfilled') {
      setDetail(detailResult.value);
    } else {
      setError(detailResult.reason?.message || 'Failed to load account detail.');
    }

    if (graphResult.status === 'fulfilled') {
      setGraph(graphResult.value);
    } else {
      setGraphError(graphResult.reason?.message || 'Failed to load graph neighborhood.');
    }

    setGraphLoading(false);
  }, []);

  const runNow = useCallback(() => {
    setRunRequesting(true);
    setRunMessage(null);
    requestNetworkRun(lookbackDays)
      .then((res) => {
        setRunMessage(res.message);
        loadScores(); // analysis already finished by the time this resolves - refresh immediately
      })
      .catch((err) => setError(err.message))
      .finally(() => setRunRequesting(false));
  }, [lookbackDays, loadScores]);

  const communityCount = new Set(scores.map((s) => s.communityId)).size;
  const flaggedCount = latestRun?.accountsFlagged ?? null;

  return (
    <div className="page-network">
      <div className="page-header">
        <h1 className="page-title">Network Insights</h1>
        {!loading && !error && (
          <span className="row-count">{scores.length.toLocaleString()} accounts scored</span>
        )}
      </div>

      <NetworkSummaryCards latestRun={latestRun} flaggedCount={flaggedCount} communityCount={communityCount || null} />

      <div className="network-toolbar">
        <label>
          Min score:{' '}
          <input
            type="number"
            min="0"
            max="100"
            value={minScore}
            onChange={(e) => setMinScore(e.target.value)}
            placeholder="0"
            style={{ width: 70 }}
          />
        </label>

        <label>
          Lookback:{' '}
          <select value={lookbackDays} onChange={(e) => setLookbackDays(Number(e.target.value))}>
            <option value={7}>7 days</option>
            <option value={30}>30 days</option>
            <option value={90}>90 days</option>
          </select>
        </label>

        <button className="btn-run-now" onClick={runNow} disabled={runRequesting}>
          {runRequesting ? 'Running… (takes a few seconds)' : 'Run Analysis Now'}
        </button>

        {runMessage && <span className="network-run-status">{runMessage}</span>}
      </div>

      {error && <div className="error-banner">Failed to load network data: {error}</div>}

      {loading
        ? <div className="loading-state">Loading network scores…</div>
        : <NetworkScoreTable rows={scores} selectedAccountId={selectedAccountId} onSelect={selectAccount} />}

      {selectedAccountId && (
        <NetworkAccountDetail
          detail={detail}
          graph={graph}
          graphLoading={graphLoading}
          graphError={graphError}
          accountTrail={accountTrail}
          onSelectNode={(accountId) => selectAccount(accountId, 'graph')}
          onSelectTrailAccount={(accountId) => selectAccount(accountId, 'graph')}
        />
      )}
    </div>
  );
}
