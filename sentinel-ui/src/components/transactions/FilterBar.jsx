/**
 * FilterBar.jsx
 *
 * Opens a centred modal overlay when the "Filters" button is clicked.
 * All fields are stubbed (disabled) until the filtering phase wires them.
 *
 * TODO (filtering phase):
 *   1. Lift filter state up to TransactionsPage (or use a context/store)
 *   2. Pass current values as props and onChange handlers down
 *   3. On "Apply", call the parent's onFilterChange({ status, type, ... })
 *   4. TransactionsPage resets page to 0 and refetches with the new params
 */
import { useEffect, useState } from 'react';
import './FilterBar.css';

export default function FilterBar() {
  const [open, setOpen] = useState(false);

  // Close on Escape key
  useEffect(() => {
    if (!open) return;
    const handler = (e) => { if (e.key === 'Escape') setOpen(false); };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, [open]);

  return (
    <>
      {/* ── Trigger button ── */}
      <div className="filter-bar">
        <button
          className="filter-toggle"
          onClick={() => setOpen(true)}
          aria-haspopup="dialog"
        >
          <svg className="filter-icon-svg" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
            <path fillRule="evenodd" d="M3 5a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1zM6 10a1 1 0 011-1h6a1 1 0 110 2H7a1 1 0 01-1-1zm3 5a1 1 0 011-1h0a1 1 0 110 2h0a1 1 0 01-1-1z" clipRule="evenodd" />
          </svg>
          Filters
        </button>
      </div>

      {/* ── Modal ── */}
      {open && (
        <div
          className="modal-backdrop"
          onClick={(e) => { if (e.target === e.currentTarget) setOpen(false); }}
          role="dialog"
          aria-modal="true"
          aria-label="Filter transactions"
        >
          <div className="filter-modal">

            {/* Header */}
            <div className="filter-modal-header">
              <h2 className="filter-modal-title">Filter Transactions</h2>
              <button className="filter-modal-close" onClick={() => setOpen(false)} aria-label="Close">
                <svg viewBox="0 0 20 20" fill="currentColor" width="18" height="18" aria-hidden="true">
                  <path fillRule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clipRule="evenodd" />
                </svg>
              </button>
            </div>

            {/* Body */}
            <div className="filter-modal-body">
              <div className="filter-coming-soon-banner">
                Filtering is coming soon — fields are shown for preview only.
              </div>

              <div className="filter-section-title">Transaction</div>
              <div className="filter-grid">
                <label className="filter-label">
                  Status
                  <select className="filter-input" disabled>
                    <option value="">All</option>
                    <option value="COMPLETED">Completed</option>
                    <option value="PENDING">Pending</option>
                    <option value="FAILED">Failed</option>
                  </select>
                </label>
                <label className="filter-label">
                  Type
                  <select className="filter-input" disabled>
                    <option value="">All</option>
                    <option value="DEBIT">Debit</option>
                    <option value="CREDIT">Credit</option>
                  </select>
                </label>
                <label className="filter-label">
                  Min Amount
                  <input type="number" className="filter-input" placeholder="0.00" step="0.01" disabled />
                </label>
                <label className="filter-label">
                  Max Amount
                  <input type="number" className="filter-input" placeholder="99 999.99" step="0.01" disabled />
                </label>
              </div>

              <div className="filter-section-title">Date Range</div>
              <div className="filter-grid">
                <label className="filter-label">
                  From
                  <input type="datetime-local" className="filter-input" disabled />
                </label>
                <label className="filter-label">
                  To
                  <input type="datetime-local" className="filter-input" disabled />
                </label>
              </div>

              <div className="filter-section-title">Account & Payee</div>
              <div className="filter-grid">
                <label className="filter-label">
                  Account ID
                  <input type="number" className="filter-input mono" placeholder="e.g. 3" disabled />
                </label>
                <label className="filter-label">
                  Payee ID
                  <input type="number" className="filter-input mono" placeholder="e.g. 7" disabled />
                </label>
              </div>
            </div>

            {/* Footer */}
            <div className="filter-modal-footer">
              <button className="btn-reset" disabled>Reset</button>
              <button className="btn-apply" disabled>Apply Filters</button>
            </div>

          </div>
        </div>
      )}
    </>
  );
}
