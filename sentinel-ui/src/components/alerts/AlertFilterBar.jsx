/**
 * AlertFilterBar.jsx
 * Modal filter panel for the alerts table.
 * Same open/draft/apply/reset pattern as FilterBar.jsx (transactions).
 */
import { useEffect, useState } from 'react';
import { EMPTY_ALERT_FILTERS, countActiveAlertFilters } from '../../utils/alertFilterUtils';
import '../transactions/FilterBar.css';

export default function AlertFilterBar({ filters, onFiltersChange }) {
  const [open, setOpen]   = useState(false);
  const [draft, setDraft] = useState(EMPTY_ALERT_FILTERS);
  const activeCount = countActiveAlertFilters(filters);

  function openModal() {
    setDraft({ ...filters });
    setOpen(true);
  }

  function closeModal() { setOpen(false); }

  function handleApply() {
    onFiltersChange({ ...draft });
    closeModal();
  }

  function handleReset() {
    setDraft(EMPTY_ALERT_FILTERS);
    onFiltersChange(EMPTY_ALERT_FILTERS);
    closeModal();
  }

  function set(field) {
    return (e) => setDraft((prev) => ({ ...prev, [field]: e.target.value }));
  }

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
          aria-label="Filter alerts"
        >
          <div className="filter-modal">

            {/* Header */}
            <div className="filter-modal-header">
              <h2 className="filter-modal-title">Filter Alerts</h2>
              <button className="filter-modal-close" onClick={closeModal} aria-label="Close">
                <svg viewBox="0 0 20 20" fill="currentColor" width="18" height="18" aria-hidden="true">
                  <path fillRule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clipRule="evenodd" />
                </svg>
              </button>
            </div>

            {/* Body */}
            <div className="filter-modal-body">

              <div className="filter-section-title">Alert</div>
              <div className="filter-grid">
                <label className="filter-label">
                  Severity
                  <select className="filter-input" value={draft.severity} onChange={set('severity')}>
                    <option value="">All</option>
                    <option value="HIGH">High</option>
                    <option value="MID">Mid</option>
                    <option value="LOW">Low</option>
                  </select>
                </label>
                <label className="filter-label">
                  Alert ID
                  <input
                    type="text"
                    className="filter-input"
                    placeholder="e.g. 1042"
                    value={draft.alertId}
                    onChange={set('alertId')}
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
                    placeholder="Payee name"
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
