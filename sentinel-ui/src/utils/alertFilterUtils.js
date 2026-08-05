/**
 * alertFilterUtils.js
 * Pure client-side filtering for the alerts table.
 *
 * Filter fields:
 *   severity      — exact match on alert.severity     (HIGH | MID | LOW)
 *   alertId       — exact match on alert.alertId
 *   fromDate      — alert.createdAt >= date
 *   toDate        — alert.createdAt <= date
 *   accountSearch — partial match on accountNumber or customerName
 *   payeeSearch   — partial match on payeeName
 */

export const EMPTY_ALERT_FILTERS = {
  severity:      '',
  alertId:       '',
  fromDate:      '',
  toDate:        '',
  accountSearch: '',
  payeeSearch:   '',
};

export function countActiveAlertFilters(filters) {
  return Object.values(filters).filter(v => v !== '' && v != null).length;
}

export function filterAlerts(alerts, filters) {
  const {
    severity,
    alertId,
    fromDate,
    toDate,
    accountSearch,
    payeeSearch,
  } = filters;

  const idFilter = alertId !== '' && alertId != null ? String(alertId).trim() : '';
  const fromMs   = fromDate ? new Date(fromDate).getTime() : null;
  const toMs     = toDate   ? new Date(toDate).getTime()   : null;
  const acctQ    = accountSearch?.trim().toLowerCase() || null;
  const payeeQ   = payeeSearch?.trim().toLowerCase()   || null;

  return alerts.filter((alert) => {
    const tx   = alert.transaction ?? {};
    const acct = tx.account  ?? {};
    const payee = tx.payee   ?? {};

    // Severity
    if (severity && alert.severity !== severity) return false;

    // Alert ID (exact match)
    if (idFilter && String(alert.alertId ?? '') !== idFilter) return false;

    // Date range on createdAt
    if (fromMs != null || toMs != null) {
      const ts = alert.createdAt ? new Date(alert.createdAt).getTime() : null;
      if (ts == null) return false;
      if (fromMs != null && ts < fromMs) return false;
      if (toMs   != null && ts > toMs)   return false;
    }

    // Account search
    if (acctQ) {
      const acctNum  = (acct.accountNumber  ?? '').toLowerCase();
      const first = (acct.customer?.firstName ?? '').toLowerCase();
      const last = (acct.customer?.lastName ?? '').toLowerCase();
      const full = `${first} ${last}`.trim();
      const legacy = (acct.customerName ?? '').toLowerCase();
      if (!acctNum.includes(acctQ) && !full.includes(acctQ) && !legacy.includes(acctQ)) return false;
    }

    // Payee search
    if (payeeQ) {
      const payeeName = (payee.payeeName ?? '').toLowerCase();
      if (!payeeName.includes(payeeQ)) return false;
    }

    return true;
  });
}
