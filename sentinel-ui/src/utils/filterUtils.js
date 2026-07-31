/**
 * filterUtils.js
 *
 * Pure client-side filtering for the transactions table.
 * All filtering runs in-memory against data already loaded — no API calls.
 *
 * filterRows(rows, filters) → filtered subset of rows
 *
 * Filter fields:
 *   status        — exact match on tx.status  (e.g. "COMPLETED")
 *   type          — exact match on tx.type    (e.g. "DEBIT")
 *   minAmount     — tx.amount >= minAmount
 *   maxAmount     — tx.amount <= maxAmount
 *   fromDate      — tx.transactionTimestamp >= fromDate  (ISO string)
 *   toDate        — tx.transactionTimestamp <= toDate    (ISO string)
 *   accountSearch — case-insensitive partial match on accountNumber OR customerName
 *   payeeSearch   — case-insensitive partial match on payeeName OR payeeIdentifier
 *
 * Any field that is null / undefined / empty string is treated as inactive
 * (all rows pass that filter).
 */

/**
 * Returns a new array containing only the rows that pass every active filter.
 *
 * @param {object[]} rows     - full array of transaction objects
 * @param {object}   filters  - filter values object (see fields above)
 * @returns {object[]}
 */
export function filterRows(rows, filters) {
  const {
    status,
    type,
    minAmount,
    maxAmount,
    fromDate,
    toDate,
    accountSearch,
    payeeSearch,
  } = filters;

  const minAmt   = minAmount  !== '' && minAmount  != null ? parseFloat(minAmount)  : null;
  const maxAmt   = maxAmount  !== '' && maxAmount  != null ? parseFloat(maxAmount)  : null;
  const fromMs   = fromDate   ? new Date(fromDate).getTime()  : null;
  const toMs     = toDate     ? new Date(toDate).getTime()    : null;
  const acctQ    = accountSearch?.trim().toLowerCase() || null;
  const payeeQ   = payeeSearch?.trim().toLowerCase()   || null;

  return rows.filter((tx) => {
    // Status
    if (status && tx.status !== status) return false;

    // Type
    if (type && tx.type !== type) return false;

    // Amount range
    const amt = tx.amount != null ? parseFloat(tx.amount) : null;
    if (minAmt != null && (amt == null || amt < minAmt)) return false;
    if (maxAmt != null && (amt == null || amt > maxAmt)) return false;

    // Date range
    const txMs = tx.transactionTimestamp
      ? new Date(tx.transactionTimestamp).getTime()
      : null;
    if (fromMs != null && (txMs == null || txMs < fromMs)) return false;
    if (toMs   != null && (txMs == null || txMs > toMs))   return false;

    // Account search (accountNumber or customerName)
    if (acctQ) {
      const num  = (tx.accountNumber ?? '').toLowerCase();
      const name = (tx.customerName  ?? '').toLowerCase();
      if (!num.includes(acctQ) && !name.includes(acctQ)) return false;
    }

    // Payee search (payeeName or payeeIdentifier)
    if (payeeQ) {
      const pname = (tx.payeeName       ?? '').toLowerCase();
      const pid   = (tx.payeeIdentifier ?? '').toLowerCase();
      if (!pname.includes(payeeQ) && !pid.includes(payeeQ)) return false;
    }

    return true;
  });
}

/** Returns the number of filter fields that are actively set. */
export function countActiveFilters(filters) {
  return Object.values(filters).filter((v) => v !== '' && v != null).length;
}

/** An empty filters object — use as the initial state. */
export const EMPTY_FILTERS = {
  status:        '',
  type:          '',
  minAmount:     '',
  maxAmount:     '',
  fromDate:      '',
  toDate:        '',
  accountSearch: '',
  payeeSearch:   '',
};
