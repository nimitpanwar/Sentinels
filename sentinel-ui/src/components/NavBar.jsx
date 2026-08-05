import { NavLink } from 'react-router-dom';
import { useEffect, useState } from 'react';
import './NavBar.css';

const NAV_ITEMS = [
  { to: '/overview', label: 'Overview', icon: '\u25A3' },
  { to: '/transactions', label: 'Transactions', icon: '\u21C4' },
  { to: '/alerts', label: 'Alerts', icon: '\u26A0' },
  { to: '/assistant', label: 'Assistant', icon: '\u2726' },
];

export default function NavBar() {
  const [now, setNow] = useState(() => new Date());
  const [isOpen, setIsOpen] = useState(false);

  useEffect(() => {
    const id = setInterval(() => setNow(new Date()), 1000);
    return () => clearInterval(id);
  }, []);

  useEffect(() => {
    function handleKeyDown(e) {
      if (e.key === 'Escape') setIsOpen(false);
    }
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, []);

  return (
    <>
      <div className="topbar">
        <button
          type="button"
          className="topbar-toggle"
          aria-label={isOpen ? 'Close navigation' : 'Open navigation'}
          aria-expanded={isOpen}
          onClick={() => setIsOpen((open) => !open)}
        >
          {isOpen ? '\u2715' : '\u2630'}
        </button>
        <span className="topbar-brand">Sentinel</span>
      </div>

      {isOpen && <div className="navbar-backdrop" onClick={() => setIsOpen(false)} />}

      <nav className={'navbar' + (isOpen ? ' navbar--open' : '')}>
        <div className="navbar-links">
          {NAV_ITEMS.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              onClick={() => setIsOpen(false)}
              className={({ isActive }) => 'navbar-link' + (isActive ? ' navbar-link--active' : '')}
            >
              <span className="navbar-link-icon" aria-hidden="true">{item.icon}</span>
              <span className="navbar-link-label">{item.label}</span>
            </NavLink>
          ))}
        </div>
      </nav>

      <div className="status-bar">
        <span className="status-bar-item">
          <span className="status-dot" aria-hidden="true" />
          CORE_SERVER: OPERATIONAL
        </span>
        <span className="status-bar-item status-bar-time">
          {now.toLocaleString(undefined, { hour12: false })}
        </span>
      </div>
    </>
  );
}

