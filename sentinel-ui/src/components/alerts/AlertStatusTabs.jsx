const TABS = [
  { key: 'ALL',       label: 'All' },
  { key: 'OPEN',      label: 'Open' },
  { key: 'IN_REVIEW', label: 'In Review' },
  { key: 'ESCALATED', label: 'Escalated' },
  { key: 'CLOSED',    label: 'Closed' },
];

export default function AlertStatusTabs({ activeTab, onTabChange }) {
  return (
    <div className="alert-status-tabs">
      {TABS.map(t => (
        <button
          key={t.key}
          className={`status-tab${activeTab === t.key ? ' status-tab--active' : ''}`}
          onClick={() => onTabChange(t.key)}
        >
          {t.label}
        </button>
      ))}
    </div>
  );
}
