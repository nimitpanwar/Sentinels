<<<<<<< HEAD
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
=======
import { useState } from 'react';
import { NavLink, Link } from 'react-router-dom';
import logo from '../assets/logo.png';
import './NavBar.css';

export default function NavBar({ drawerOpen, setDrawerOpen }) {

  const links = [
    { to: '/overview', label: 'Overview', code: '01' },
    { to: '/transactions', label: 'Transactions', code: '02' },
    { to: '/alerts', label: 'Alerts', code: '03' },
    { to: '/alert-history', label: 'Alert History', code: '04' },
    { to: '/network', label: 'Network Insights', code: '05' },
    { to: '/agent', label: 'Agent', code: '06' },
  ];

  return (
    <>
      <header className="cc-topbar">
        <div className="cc-topbar__left">
          <Link to="/overview" className="cc-topbar__logo-link" aria-label="Go to Overview">
            <img src={logo} alt="Sentinel" className="cc-topbar__logo" />
          </Link>
          <button
            type="button"
            className="cc-menu-toggle"
            onClick={() => setDrawerOpen(open => !open)}
            aria-label="Toggle navigation"
            aria-expanded={drawerOpen}
          >
            <span />
            <span />
            <span />
          </button>
        </div>
        <div className="cc-topbar__titleblock">
          <div className="cc-topbar__eyebrow">Sentinel Command Center</div>
          <div className="cc-topbar__title">Fraud Monitoring Operations</div>
        </div>
        <div className="cc-topbar__meta">
          <span className="cc-signal-dot" />
          Live Workspace
        </div>
      </header>

      <div className={`cc-drawer-backdrop ${drawerOpen ? 'is-open' : ''}`} onClick={() => setDrawerOpen(false)} />

      <nav className={`cc-drawer ${drawerOpen ? 'is-open' : ''}`}>
        <div className="cc-drawer__brand">
          <div className="cc-drawer__brand-mark">S</div>
          <div>
            <div className="cc-drawer__brand-title">Sentinel</div>
            <div className="cc-drawer__brand-subtitle">Command Center</div>
          </div>
        </div>

        <div className="cc-drawer__section-label">Navigation</div>
        <div className="cc-drawer__links">
          {links.map((link) => (
            <NavLink
              key={link.to}
              to={link.to}
              onClick={() => setDrawerOpen(false)}
              className={({ isActive }) => `cc-drawer__link${isActive ? ' cc-drawer__link--active' : ''}`}
            >
              <span className="cc-drawer__link-code">{link.code}</span>
              <span>{link.label}</span>
            </NavLink>
          ))}
        </div>
      </nav>

      <div className="cc-statusbar">
        <span>Interface Mode: Command Center</span>
        <span>Scope: Overview, Alerts, Transactions, History, Network</span>
      </div>
>>>>>>> master
    </>
  );
}

