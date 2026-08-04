export default function AlertSummaryCards({ alerts }) {
  const today = new Date().toDateString();

  const open      = alerts.filter(a => a.status === 'OPEN').length;
  const inReview  = alerts.filter(a => a.status === 'ACKNOWLEDGED').length;
  const escalated = alerts.filter(a => a.status === 'INVESTIGATING').length;
  const todayCount = alerts.filter(a => {
    if (!a.createdAt) return false;
    return new Date(a.createdAt).toDateString() === today;
  }).length;

  const cards = [
    { key: 'open',      label: 'Open',       value: open,       mod: 'open' },
    { key: 'review',    label: 'In Review',  value: inReview,   mod: 'review' },
    { key: 'escalated', label: 'Escalated',  value: escalated,  mod: 'escalated' },
    { key: 'today',     label: 'Today',      value: todayCount, mod: 'today' },
  ];

  return (
    <div className="alert-cards">
      {cards.map(c => (
        <div key={c.key} className={`alert-card alert-card--${c.mod}`}>
          <div className="alert-card__label">{c.label}</div>
          <div className="alert-card__value">{c.value}</div>
        </div>
      ))}
    </div>
  );
}
