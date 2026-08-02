import { useState } from 'react';
import TransactionsPage from './components/transactions/TransactionsPage';
import NetworkPage from './components/network/NetworkPage';

const TABS = [
  { id: 'transactions', label: 'Transactions', Component: TransactionsPage },
  { id: 'network', label: 'Network Insights', Component: NetworkPage },
];

export default function App() {
  const [activeTab, setActiveTab] = useState('transactions');
  const ActiveComponent = TABS.find((t) => t.id === activeTab).Component;

  return (
    <div>
      <nav className="app-nav">
        {TABS.map((tab) => (
          <button
            key={tab.id}
            className={`app-nav-tab${activeTab === tab.id ? ' is-active' : ''}`}
            onClick={() => setActiveTab(tab.id)}
          >
            {tab.label}
          </button>
        ))}
      </nav>
      <ActiveComponent />
    </div>
  );
}