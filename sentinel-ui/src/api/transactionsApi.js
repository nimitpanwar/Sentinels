/**
 * transactionsApi.js
 *
 * Thin wrapper around the backend GET /api/transactions endpoint.
 * The Vite proxy forwards /api/* to http://localhost:8080, so no
 * base URL needs to be configured here.
 *
 * Spring's Page response shape:
 * {
 *   content: [ ...TransactionResponse objects... ],
 *   totalElements: 3847,
 *   totalPages: 77,
 *   last: false,      // true when this is the final page
 *   number: 0,        // current page index (0-based)
 *   size: 50
 * }
 */

/**
 * Fetches one page of transactions, sorted newest-first.
 *
 * @param {number} page  0-based page index
 * @param {number} size  rows per page (default 50)
 * @returns {Promise<object>}  the Spring Page wrapper object
 */
export async function fetchTransactions(page = 0, size = 50) {
  const params = new URLSearchParams({
    page,
    size,
    sort: 'transactionTimestamp,desc',
  });

  const res = await fetch(`/api/transactions?${params}`);
  if (!res.ok) {
    throw new Error(`Failed to load transactions (HTTP ${res.status})`);
  }
  return res.json();
}
