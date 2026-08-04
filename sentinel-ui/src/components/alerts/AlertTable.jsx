import AlertRow from './AlertRow';

const COLUMNS = [
  { label: 'Alert ID',      cls: 'mono' },
  { label: 'Severity',      cls: '' },
  { label: 'Status',        cls: '' },
  { label: 'Risk Score',    cls: 'num' },
  { label: 'Account No.',   cls: 'mono' },
  { label: 'Customer',      cls: '' },
  { label: 'Payee',         cls: '' },
  { label: 'Amount',        cls: 'num' },
  { label: 'Created At',    cls: '' },
];

export default function AlertTable({ alerts }) {
  if (!alerts.length) {
    return <div className="alerts-empty">No alerts found.</div>;
  }

  return (
    <div className="alerts-table-wrap">
      <table className="alerts-table">
        <thead>
          <tr>
            {COLUMNS.map(c => (
              <th key={c.label} className={c.cls}>{c.label}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {alerts.map(alert => (
            <AlertRow key={alert.alertId} alert={alert} />
          ))}
        </tbody>
      </table>
    </div>
  );
}
