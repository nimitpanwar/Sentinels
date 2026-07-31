/**
 * TransactionRow.jsx
 *
 * A single table row. Clicking anywhere on the row opens a modal overlay
 * showing the full TransactionExpandedDetail for that transaction.
 */
import { useEffect, useState } from 'react';
import TransactionExpandedDetail from './TransactionExpandedDetail';
import './transactions.css';

function Badge({ value }) {
  if (!value) return <span className="badge badge--none">—</span>;
  return <span className={`badge badge--${value.toLowerCase()}`}>{value}</span>;
}

function fmt(ts) {
  if (!ts) return '—';
  return ts.replace('T', ' ').slice(0, 19);
}

function TransactionDetailModal({ tx, onClose }) {
  // Close on Escape key
  useEffect(() => {
    const handler = (e) => { if (e.key === 'Escape') onClose(); };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, [onClose]);

  return (
    <div
      className="modal-backdrop"
      onClick={(e) => { if (e.target === e.currentTarget) onClose(); }}
      role="dialog"
      aria-modal="true"
      aria-label={`Transaction ${tx.transactionId}`}
    >
      <div className="detail-modal">

        {/* Header */}
        <div className="detail-modal-header">
          <div className="detail-modal-title-group">
            <span className="detail-modal-label">Transaction</span>
            <span className="detail-modal-id mono">#{tx.transactionId}</span>
          </div>
          <div className="detail-modal-header-right">
            <Badge value={tx.status} />
            <Badge value={tx.type} />
            <button className="filter-modal-close" onClick={onClose} aria-label="Close">
              <svg viewBox="0 0 20 20" fill="currentColor" width="18" height="18" aria-hidden="true">
                <path fillRule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clipRule="evenodd" />
              </svg>
            </button>
          </div>
        </div>

        {/* Body */}
        <div className="detail-modal-body">
          <TransactionExpandedDetail tx={tx} />
        </div>

      </div>
    </div>
  );
}

export default function TransactionRow({ tx }) {
  const [open, setOpen] = useState(false);

  return (
    <>
      <tr
        className="tx-row"
        onClick={() => setOpen(true)}
        title="Click to view details"
      >
        {/* Expand indicator */}
        <td className="tx-cell tx-cell--expand" aria-label="Open details">
          <span className="chevron-cell">▶</span>
        </td>

        <td className="tx-cell mono">{tx.transactionId ?? '—'}</td>
        <td className="tx-cell">{fmt(tx.transactionTimestamp)}</td>
        <td className="tx-cell mono">{tx.accountNumber ?? '—'}</td>
        <td className="tx-cell">{tx.payeeName ?? '—'}</td>
        <td className="tx-cell num">
          {tx.amount != null ? Number(tx.amount).toFixed(2) : '—'}
          {' '}
          <span className="currency">{tx.currency ?? ''}</span>
        </td>
        <td className="tx-cell"><Badge value={tx.type} /></td>
        <td className="tx-cell"><Badge value={tx.status} /></td>
        <td className="tx-cell num">
          {tx.riskScore != null
            ? <span className="risk-score" data-score={tx.riskScore}>{tx.riskScore}</span>
            : <span className="muted">—</span>}
        </td>
      </tr>

      {open && <TransactionDetailModal tx={tx} onClose={() => setOpen(false)} />}
    </>
  );
}
