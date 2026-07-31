/**
 * TransactionsPage.jsx
 *
 * Root page for the transactions list.
 *
 * Load strategy:
 *   - On mount, fetches pages 0 and 1 concurrently (2 × 50 = 100 initial rows).
 *   - Each "Show More" click fetches the next page (50 rows) and appends to state.
 *   - When Spring returns { last: true }, the "Show More" button is hidden.
 *
 * State:
 *   rows        — accumulated transaction objects (all pages merged)
 *   nextPage    — the next page index to fetch on "Show More"
 *   hasMore     — whether more pages exist (driven by Spring's `last` field)
 *   loading     — true while any fetch is in flight
 *   error       — non-null string if a fetch failed
 */
import { useEffect, useState, useCallback, useMemo } from 'react';
import { fetchTransactions } from '../../api/transactionsApi';
import { filterRows, countActiveFilters, EMPTY_FILTERS } from '../../utils/filterUtils';
import FilterBar from './FilterBar';
import TransactionTable from './TransactionTable';
import './transactions.css';

const INITIAL_SIZE = 50;   // rows per concurrent initial request (×2 = 100 total)
const MORE_SIZE    = 50;   // rows per "Show More" request

export default function TransactionsPage() {
  const [rows, setRows]         = useState([]);
  const [nextPage, setNextPage] = useState(2);
  const [hasMore, setHasMore]   = useState(true);
  const [loading, setLoading]   = useState(true);
  const [error, setError]       = useState(null);
  const [filters, setFilters]   = useState(EMPTY_FILTERS);

  // Derive filtered rows from the full loaded set — no fetch needed
  const filteredRows   = useMemo(() => filterRows(rows, filters), [rows, filters]);
  const activeFilters  = countActiveFilters(filters);

  // Initial load: fetch pages 0 and 1 in parallel → 100 rows
  useEffect(() => {
    let cancelled = false;
    setLoading(true);

    Promise.all([
      fetchTransactions(0, INITIAL_SIZE),
      fetchTransactions(1, INITIAL_SIZE),
    ])
      .then(([page0, page1]) => {
        if (cancelled) return;
        // Deduplicate in case the DB has fewer than 100 rows (page1.content may overlap or be empty)
        const seen = new Set();
        const merged = [...page0.content, ...page1.content].filter((tx) => {
          if (seen.has(tx.transactionId)) return false;
          seen.add(tx.transactionId);
          return true;
        });
        setRows(merged);
        // If either page was the last, there are no more pages
        setHasMore(!page1.last && page1.content.length === INITIAL_SIZE);
        setNextPage(2);
      })
      .catch((err) => {
        if (!cancelled) setError(err.message);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => { cancelled = true; };
  }, []);

  // "Show More" handler: fetch one more page and append
  const loadMore = useCallback(() => {
    if (loading || !hasMore) return;
    setLoading(true);

    fetchTransactions(nextPage, MORE_SIZE)
      .then((page) => {
        setRows((prev) => {
          const seen = new Set(prev.map((t) => t.transactionId));
          const fresh = page.content.filter((t) => !seen.has(t.transactionId));
          return [...prev, ...fresh];
        });
        setHasMore(!page.last && page.content.length === MORE_SIZE);
        setNextPage((p) => p + 1);
      })
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, [loading, hasMore, nextPage]);

  return (
    <div className="page-transactions">
      <div className="page-header">
        <h1 className="page-title">Transactions</h1>
        {!loading && !error && (
          <span className="row-count">
            {activeFilters > 0
              ? <>{filteredRows.length.toLocaleString()} of {rows.length.toLocaleString()} loaded</>
              : <>{rows.length.toLocaleString()} loaded</>}
          </span>
        )}
      </div>

      <FilterBar filters={filters} onFiltersChange={setFilters} />

      {error && (
        <div className="error-banner">
          Failed to load transactions: {error}
        </div>
      )}

      {loading && rows.length === 0
        ? <div className="loading-state">Loading transactions…</div>
        : <TransactionTable rows={filteredRows} />}

      {/* Show More / loading spinner / end-of-list */}
      <div className="pagination-bar">
        {loading && rows.length > 0 && (
          <span className="loading-more">Loading…</span>
        )}
        {!loading && hasMore && (
          <button className="btn-show-more" onClick={loadMore}>
            Show More
          </button>
        )}
        {!loading && !hasMore && rows.length > 0 && (
          <span className="end-of-list">
            {activeFilters > 0
              ? `${filteredRows.length.toLocaleString()} matches — all ${rows.length.toLocaleString()} transactions searched`
              : `All ${rows.length.toLocaleString()} transactions loaded`}
          </span>
        )}
      </div>
    </div>
  );
}
