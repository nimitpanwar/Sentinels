/**
 * FilterBar.jsx
 *
 * Modal filter panel wired to client-side filtering.
 *
 * Props:
 *   filters          — current active filters object (from TransactionsPage)
 *   onFiltersChange  — callback(newFilters) called when user clicks Apply or Reset
 *
 * Pattern: draft state inside the modal.
 *   - Opening the modal seeds draft from the current active filters.
 *   - Edits update draft only.
 *   - "Apply"  → commits draft to parent via onFiltersChange, closes modal.
 *   - "Reset"  → clears draft AND calls onFiltersChange(EMPTY_FILTERS), closes modal.
 *   - Closing without Apply discards the draft (active filters unchanged).
 */
import { useEffect, useState } from 'react';
import { EMPTY_FILTERS, countActiveFilters } from '../../utils/filterUtils';
import './FilterBar.css';

export default function FilterBar({ filters, onFiltersChange }) {
  const [open, setOpen]           = useState(false);
  const [draft, setDraft]         = useState(EMPTY_FILTERS);
  const activeCount = countActiveFilters(filters);

  // Seed draft from current active filters when the modal opens
  function openModal() {
    setDraft({ ...filters });
    setOpen(true);
  }

  function closeModal() {
    setOpen(false);
  }

  function handleApply() {
    onFiltersChange({ ...draft });
    closeModal();
  }

  function handleReset() {
    setDraft(EMPTY_FILTERS);
    onFiltersChange(EMPTY_FILTERS);
    closeModal();
  }

  function set(field) {
    return (e) => setDraft((prev) => ({ ...prev, [field]: e.target.value }));
  }

  // Close on Escape
  useEffect(() => {
    if (!open) return;
    const handler = (e) => { if (e.key === 'Escape') closeModal(); };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, [open]);

  return (
    <>
      {/* ── Trigger button ── */}
      <div className="filter-bar">
        <button
          className={`filter-toggle ${activeCount > 0 ? 'filter-toggle--active' : ''}`}
          onClick={openModal}
          aria-haspopup="dialog"
        >
          <svg className="filter-icon-svg" viewBox="0 0 20 20" fill="currentColor" aria-hidden="true">
            <path fillRule="evenodd" d="M3 5a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1zM6 10a1 1 0 011-1h6a1 1 0 110 2H7a1 1 0 01-1-1zm3 5a1 1 0 011-1h0a1 1 0 110 2h0a1 1 0 01-1-1z" clipRule="evenodd" />
          </svg>
          Filters
          {activeCount > 0 && (
            <span className="filter-badge">{activeCount}</span>
          )}
        </button>
      </div>

      {/* ── Modal ── */}
      {open && (
        <div
          className="modal-backdrop"
          onClick={(e) => { if (e.target === e.currentTarget) closeModal(); }}
          role="dialog"
          aria-modal="true"
          aria-label="Filter transactions"
        >
          <div className="filter-modal">

            {/* Header */}
            <div className="filter-modal-header">
              <h2 className="filter-modal-title">Filter Transactions</h2>
              <button className="filter-modal-close" onClick={closeModal} aria-label="Close">
                <svg viewBox="0 0 20 20" fill="currentColor" width="18" height="18" aria-hidden="true">
                  <path fillRule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clipRule="evenodd" />
                </svg>
              </button>
            </div>

            {/* Body */}
            <div className="filter-modal-body">

              <div className="filter-section-title">Transaction</div>
              <div className="filter-grid">
                <label className="filter-label">
                  Status
                  <select className="filter-input" value={draft.status} onChange={set('status')}>
                    <option value="">All</option>
                    <option value="COMPLETED">Completed</option>
                    <option value="PENDING">Pending</option>
                    <option value="FAILED">Failed</option>
                  </select>
                </label>
                <label className="filter-label">
                  Type
                  <select className="filter-input" value={draft.type} onChange={set('type')}>
                    <option value="">All</option>
                    <option value="DEBIT">Debit</option>
                    <option value="CREDIT">Credit</option>
                  </select>
                </label>
                <label className="filter-label">
                  Min Amount
                  <input
                    type="number"
                    className="filter-input"
                    placeholder="0.00"
                    step="0.01"
                    value={draft.minAmount}
                    onChange={set('minAmount')}
                  />
                </label>
                <label className="filter-label">
                  Max Amount
                  <input
                    type="number"
                    className="filter-input"
                    placeholder="99 999.99"
                    step="0.01"
                    value={draft.maxAmount}
                    onChange={set('maxAmount')}
                  />
                </label>
              </div>

              <div className="filter-section-title">Date Range</div>
              <div className="filter-grid">
                <label className="filter-label">
                  From
                  <input
                    type="datetime-local"
                    className="filter-input"
                    value={draft.fromDate}
                    onChange={set('fromDate')}
                  />
                </label>
                <label className="filter-label">
                  To
                  <input
                    type="datetime-local"
                    className="filter-input"
                    value={draft.toDate}
                    onChange={set('toDate')}
                  />
                </label>
              </div>

              <div className="filter-section-title">Account &amp; Payee</div>
              <div className="filter-grid">
                <label className="filter-label">
                  Account
                  <input
                    type="text"
                    className="filter-input mono"
                    placeholder="Account no. or customer name"
                    value={draft.accountSearch}
                    onChange={set('accountSearch')}
                  />
                </label>
                <label className="filter-label">
                  Payee
                  <input
                    type="text"
                    className="filter-input"
                    placeholder="Payee name or identifier"
                    value={draft.payeeSearch}
                    onChange={set('payeeSearch')}
                  />
                </label>
              </div>

            </div>

            {/* Footer */}
            <div className="filter-modal-footer">
              <button className="btn-reset" onClick={handleReset}>Reset</button>
              <button className="btn-apply" onClick={handleApply}>Apply Filters</button>
            </div>

          </div>
        </div>
      )}
    </>
  );
}
