const TABS = [
  { key: 'ALL',       label: 'All' },
  { key: 'OPEN',      label: 'Open' },
  { key: 'ACKNOWLEDGED', label: 'In Review' },
  { key: 'INVESTIGATING', label: 'Investigating' },
  { key: 'ESCALATED', label: 'Escalated' },
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
