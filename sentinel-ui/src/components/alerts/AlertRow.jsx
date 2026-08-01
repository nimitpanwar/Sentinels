import { useNavigate } from 'react-router-dom';
import { updateAlertStatus } from '../../api/alertsApi';

function severityBadge(severity) {
  if (!severity) return null;
  const cls = severity.toLowerCase(); // high | mid | low
  return <span className={`badge badge--${cls}`}>{severity}</span>;
}

function statusBadge(status) {
  if (!status) return null;
  const clsMap = {
    OPEN:         'open',
    ACKNOWLEDGED: 'acknowledged',
    INVESTIGATING:'investigating',
    DISMISSED:    'dismissed',
    IN_REVIEW:    'in-review',
    ESCALATED:    'escalated',
    CLOSED:       'closed',
  };
  const cls = clsMap[status] ?? status.toLowerCase();
  const label = status.replace(/_/g, ' ');
  return <span className={`badge badge--${cls}`}>{label}</span>;
}

function formatDate(iso) {
  if (!iso) return '—';
  const d = new Date(iso);
  return d.toLocaleString('en-GB', {
    day: '2-digit', month: 'short', year: 'numeric',
    hour: '2-digit', minute: '2-digit',
  });
}

function formatAmount(amount) {
  if (amount == null) return '—';
  return new Intl.NumberFormat('en-US', {
    style: 'currency', currency: 'USD', minimumFractionDigits: 2,
  }).format(amount);
}

export default function AlertRow({ alert }) {
  const navigate = useNavigate();
  const tx   = alert.transaction ?? {};
  const acct = tx.account ?? {};
  const payee = tx.payee ?? {};

  async function handleClick() {
    try {
      if (alert.status === 'OPEN') {
        await updateAlertStatus(alert.alertId, 'ACKNOWLEDGED');
      }
    } catch {
      // acknowledge failed — still navigate so the operator can see the alert
    }
    navigate(`/alerts/${alert.alertId}`);
  }

  return (
    <tr onClick={handleClick}>
      <td className="mono">{alert.alertId}</td>
      <td>{severityBadge(alert.severity)}</td>
      <td>{statusBadge(alert.status)}</td>
      <td className="num">{alert.riskScore ?? '—'}</td>
      <td className="mono">{acct.accountNumber ?? '—'}</td>
      <td>{acct.customerName ?? '—'}</td>
      <td>{payee.payeeName ?? '—'}</td>
      <td className="num">{formatAmount(tx.amount)}</td>
      <td>{formatDate(alert.createdAt)}</td>
    </tr>
  );
}
