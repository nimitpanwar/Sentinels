/**
 * TransactionTable.jsx
 *
 * Renders the <table> with a sticky header and one TransactionRow per transaction.
 * Column alignment mirrors financial table conventions:
 *   - Numeric columns (Amount, Risk Score) → right-aligned header + cell
 *   - ID / account number columns → monospace
 *   - Everything else → left-aligned
 */
import TransactionRow from './TransactionRow';
import './transactions.css';

export default function TransactionTable({ rows }) {
  if (rows.length === 0) {
    return <p className="empty-state">No transactions found.</p>;
  }

  return (
    <div className="table-wrapper">
      <table className="tx-table">
        <thead>
          <tr>
            <th className="th th--expand" aria-label="Expand" />
            <th className="th mono">TX ID</th>
            <th className="th">Timestamp</th>
            <th className="th mono">Account No.</th>
            <th className="th">Payee</th>
            <th className="th th--num">Amount</th>
            <th className="th">Type</th>
            <th className="th">Status</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((tx) => (
            <TransactionRow key={tx.transactionId} tx={tx} />
          ))}
        </tbody>
      </table>
    </div>
  );
}
