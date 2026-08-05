import { NavLink } from 'react-router-dom';
import './NavBar.css';

export default function NavBar() {
  return (
    <nav className="navbar">
      <span className="navbar-brand">Sentinel</span>
      <div className="navbar-links">
        <NavLink
          to="/transactions"
          className={({ isActive }) => 'navbar-link' + (isActive ? ' navbar-link--active' : '')}
        >
          Transactions
        </NavLink>
        <NavLink
          to="/alerts"
          className={({ isActive }) => 'navbar-link' + (isActive ? ' navbar-link--active' : '')}
        >
          Alerts
        </NavLink>
        <NavLink
          to="/alert-history"
          className={({ isActive }) => 'navbar-link' + (isActive ? ' navbar-link--active' : '')}
        >
          Alert History
        </NavLink>
        <NavLink
          to="/network"
          className={({ isActive }) => 'navbar-link' + (isActive ? ' navbar-link--active' : '')}
        >
          Network Insights
        </NavLink>
      </div>
    </nav>
  );
}
