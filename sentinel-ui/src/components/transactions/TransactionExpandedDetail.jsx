/**
 * TransactionExpandedDetail.jsx
 *
 * The full detail panel rendered inside an expanded table row.
 * Shows four named sections: Transaction, Account, Payee, Risk & Alerts.
 *
 * Rules applied here:
 *   - All IDs and account/payee identifier strings use the "mono" class → monospace font
 *   - All numeric values (amount, risk score) use the "num" class → right-aligned, tabular figures
 *   - Badges for status/type/severity use "badge badge--*" classes
 *   - null / undefined values render as a dash (—) via the val() helper
 */
import './transactions.css';

function val(v) {
  return v !== null && v !== undefined && v !== '' ? v : '—';
}

function Badge({ value, type }) {
  if (!value) return <span className="badge badge--none">—</span>;
  const cls = `badge badge--${(type ?? value).toLowerCase()}`;
  return <span className={cls}>{value}</span>;
}

function Row({ label, children }) {
  return (
    <div className="detail-row">
      <span className="detail-label">{label}</span>
      <span className="detail-value">{children}</span>
    </div>
  );
}

function Section({ title, children }) {
  return (
    <div className="detail-section">
      <h4 className="detail-section-title">{title}</h4>
      {children}
    </div>
  );
}

export default function TransactionExpandedDetail({ tx }) {
  return (
    <div className="expanded-detail">

      {/* ── 1. Transaction ─────────────────────────────────────────── */}
      <Section title="Transaction">
        <Row label="ID"><span className="mono">{val(tx.transactionId)}</span></Row>
        <Row label="Timestamp">{val(tx.transactionTimestamp?.replace('T', ' '))}</Row>
        <Row label="Created At">{val(tx.createdAt?.replace('T', ' '))}</Row>
        <Row label="Status"><Badge value={tx.status} /></Row>
        <Row label="Type"><Badge value={tx.type} type={tx.type} /></Row>
        <Row label="Amount">
          <span className="num">{tx.amount != null ? Number(tx.amount).toFixed(2) : '—'}</span>
          {' '}{val(tx.currency)}
        </Row>
        <Row label="Description">{val(tx.description)}</Row>
        <Row label="Location">{val(tx.location)}</Row>
        <Row label="Merchant Category">{val(tx.merchantCategory)}</Row>
      </Section>

      {/* ── 2. Account ─────────────────────────────────────────────── */}
      <Section title="Account">
        <Row label="Account ID"><span className="mono">{val(tx.accountId)}</span></Row>
        <Row label="Account Number"><span className="mono">{val(tx.accountNumber)}</span></Row>
        <Row label="Account Type">{val(tx.accountType)}</Row>
        <Row label="Account Status"><Badge value={tx.accountStatus} /></Row>
        <Row label="Customer">{val(tx.customerName)}</Row>
      </Section>

      {/* ── 3. Payee ───────────────────────────────────────────────── */}
      <Section title="Payee">
        <Row label="Payee ID"><span className="mono">{val(tx.payeeId)}</span></Row>
        <Row label="Payee Name">{val(tx.payeeName)}</Row>
        <Row label="Payee Identifier"><span className="mono">{val(tx.payeeIdentifier)}</span></Row>
      </Section>

      {/* ── 4. Risk & Alerts ── COMMENTED OUT ─────────────────────────
      <Section title="Risk & Alerts">
        <Row label="Risk Score">
          <span className="num risk-score" data-score={tx.riskScore ?? 0}>
            {tx.riskScore != null ? tx.riskScore : '—'}
          </span>
        </Row>
        <Row label="Triggered Rules">
          {tx.triggeredRules?.length
            ? <ul className="rule-list">{tx.triggeredRules.map((r) => <li key={r}>{r}</li>)}</ul>
            : '—'}
        </Row>
        <Row label="Evidence">
          {tx.evidence?.length
            ? <ul className="rule-list">{tx.evidence.map((e, i) => <li key={i}>{e}</li>)}</ul>
            : '—'}
        </Row>
        <Row label="Alert ID">
          {tx.alertId != null ? <span className="mono">{tx.alertId}</span> : '—'}
        </Row>
        <Row label="Alert Severity"><Badge value={tx.alertSeverity} /></Row>
        <Row label="Alert Status"><Badge value={tx.alertStatus} /></Row>
        <Row label="Case ID">
          {tx.caseId != null ? <span className="mono">{tx.caseId}</span> : '—'}
        </Row>
        <Row label="Case Severity"><Badge value={tx.caseSeverity} /></Row>
        <Row label="Case Status"><Badge value={tx.caseStatus} /></Row>
      </Section>
      ── END Risk & Alerts ── */}

    </div>
  );
}
