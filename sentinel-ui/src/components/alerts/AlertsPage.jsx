import { useState, useEffect, useMemo } from 'react';
import * as XLSX from 'xlsx';
import AlertSummaryCards from './AlertSummaryCards';
import AlertStatusTabs from './AlertStatusTabs';
import AlertTable from './AlertTable';
import AlertFilterBar from './AlertFilterBar';
import { filterAlerts, EMPTY_ALERT_FILTERS } from '../../utils/alertFilterUtils';
import './alerts.css';
import '../transactions/transactions.css';

const PAGE_SIZE = 20;

export default function AlertsPage({ alerts, loading, error, onMount }) {
  const [activeTab, setActiveTab]       = useState('ALL');
  const [visibleCount, setVisibleCount] = useState(PAGE_SIZE);
  const [filters, setFilters]           = useState(EMPTY_ALERT_FILTERS);
  const [exportFormat, setExportFormat] = useState('csv');

  useEffect(() => {
    onMount();
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  function handleTabChange(tab) {
    setActiveTab(tab);
    setVisibleCount(PAGE_SIZE);
  }

  function handleFiltersChange(newFilters) {
    setFilters(newFilters);
    setVisibleCount(PAGE_SIZE);
  }

  // 1. Apply modal filters across the full list
  // 2. Apply tab filter on top
  const filteredAlerts = useMemo(() => {
    const afterFilters = filterAlerts(alerts, filters);
    if (activeTab === 'ALL') return afterFilters;
    return afterFilters.filter(a => a.status === activeTab);
  }, [alerts, filters, activeTab]);

  const visibleAlerts = filteredAlerts.slice(0, visibleCount);
  const hasMore = visibleCount < filteredAlerts.length;
  const isEscalatedTab = activeTab === 'ESCALATED';

  function csvEscape(value) {
    if (value == null) return '';
    const asString = String(value);
    if (/[",\n]/.test(asString)) {
      return `"${asString.replace(/"/g, '""')}"`;
    }
    return asString;
  }

  function customerDisplayName(account) {
    if (!account) return '';
    const first = account.customer?.firstName?.trim() ?? '';
    const last = account.customer?.lastName?.trim() ?? '';
    const full = `${first} ${last}`.trim();
    if (full) return full;
    return account.customerName?.trim() ?? '';
  }

  function buildWorksheetColumns(dataset, headers) {
    return headers.map((header) => {
      let maxLen = header.length;
      for (const row of dataset) {
        const value = row[header];
        const len = value == null ? 0 : String(value).length;
        if (len > maxLen) {
          maxLen = len;
        }
      }

      // Keep widths readable without becoming excessively wide.
      return { wch: Math.min(Math.max(maxLen + 2, 12), 42) };
    });
  }

  function applyWorksheetFormatting(worksheet) {
    if (!worksheet['!ref']) return;

    const range = XLSX.utils.decode_range(worksheet['!ref']);
    const headerRow = range.s.r;

    for (let col = range.s.c; col <= range.e.c; col += 1) {
      const address = XLSX.utils.encode_cell({ r: headerRow, c: col });
      const cell = worksheet[address];
      if (!cell) continue;

      cell.s = {
        ...(cell.s || {}),
        font: {
          ...((cell.s && cell.s.font) || {}),
          bold: true,
        },
      };
    }

    const topLeft = XLSX.utils.encode_cell({ r: range.s.r, c: range.s.c });
    const bottomRight = XLSX.utils.encode_cell({ r: range.e.r, c: range.e.c });
    worksheet['!autofilter'] = { ref: `${topLeft}:${bottomRight}` };
    worksheet['!freeze'] = { xSplit: 0, ySplit: 1, topLeftCell: 'A2', activePane: 'bottomLeft', state: 'frozen' };
  }

  function exportEscalatedAlerts() {
    const rows = filteredAlerts.filter(a => a.status === 'ESCALATED');
    if (!rows.length) {
      return;
    }

    const dataset = [];
    for (const alert of rows) {
      const tx = alert.transaction ?? {};
      const acct = tx.account ?? {};
      const payee = tx.payee ?? {};
      dataset.push({
        'Alert ID': alert.alertId ?? '',
        'Case ID': alert.case?.caseId ?? '',
        Severity: alert.severity ?? '',
        Status: alert.status ?? '',
        'Risk Score': alert.riskScore ?? '',
        'Created At (UTC)': alert.createdAt ?? '',
        'Acknowledged At (UTC)': alert.acknowledgedAt ?? '',
        'Account Number': acct.accountNumber ?? '',
        'Customer Name': customerDisplayName(acct),
        'Customer Email': acct.customer?.email ?? '',
        Payee: payee.payeeName ?? '',
        Amount: tx.amount ?? '',
        'Transaction Type': tx.type ?? '',
        'Transaction Timestamp (UTC)': tx.transactionTimestamp ?? '',
        Location: tx.location ?? '',
        Description: tx.description ?? '',
        'Resolution Notes': alert.resolutionNotes ?? '',
      });
    }

    const stamp = new Date().toISOString().replace(/[:]/g, '-').replace(/\..+/, '');

    if (exportFormat === 'xlsx') {
      const worksheet = XLSX.utils.json_to_sheet(dataset);
      const headers = Object.keys(dataset[0]);
      worksheet['!cols'] = buildWorksheetColumns(dataset, headers);
      applyWorksheetFormatting(worksheet);

      const workbook = XLSX.utils.book_new();
      XLSX.utils.book_append_sheet(workbook, worksheet, 'Escalated Alerts');
      XLSX.writeFile(workbook, `escalated-alerts-${stamp}.xlsx`, { cellStyles: true });
      return;
    }

    const headers = Object.keys(dataset[0]);
    const lines = [headers.map(csvEscape).join(',')];
    for (const row of dataset) {
      lines.push(headers.map(key => csvEscape(row[key])).join(','));
    }

    const csv = `\uFEFF${lines.join('\n')}`;
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `escalated-alerts-${stamp}.csv`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  }

  return (
    <div className="page-alerts">
      <div className="page-header">
        <h1 className="page-title">Alerts</h1>
        {!loading && !error && (
          <div className="alerts-header-right">
            <span className="row-count">
              Showing {visibleAlerts.length} of {filteredAlerts.length} alert{filteredAlerts.length !== 1 ? 's' : ''}
            </span>
          </div>
        )}
      </div>

      {loading && <div className="alerts-loading">Loading alerts…</div>}
      {error   && <div className="alerts-empty">Error: {error}</div>}

      {!loading && !error && (
        <>
          <AlertSummaryCards alerts={alerts} />
          <AlertFilterBar filters={filters} onFiltersChange={handleFiltersChange} />
          <div className="alerts-tab-export-row">
            <AlertStatusTabs activeTab={activeTab} onTabChange={handleTabChange} />
            {isEscalatedTab && (
              <div className="alerts-export-controls">
                <label className="alerts-export-label" htmlFor="alerts-export-format">Export format</label>
                <select
                  id="alerts-export-format"
                  className="filter-input alerts-export-format"
                  value={exportFormat}
                  onChange={e => setExportFormat(e.target.value)}
                >
                  <option value="csv">.csv</option>
                  <option value="xlsx">.xlsx</option>
                </select>
                <button
                  className="btn btn--secondary"
                  onClick={exportEscalatedAlerts}
                  disabled={filteredAlerts.filter(a => a.status === 'ESCALATED').length === 0}
                  type="button"
                >
                  Export Escalated Alerts
                </button>
              </div>
            )}
          </div>
          <AlertTable alerts={visibleAlerts} />

          {hasMore && (
            <div className="alerts-show-more">
              <button
                className="btn-show-more"
                onClick={() => setVisibleCount(c => c + PAGE_SIZE)}
              >
                Show More
              </button>
            </div>
          )}
        </>
      )}
    </div>
  );
}
