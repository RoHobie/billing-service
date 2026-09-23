import React from 'react';
import { User } from '../types';

interface NavbarProps {
  user: User;
  onLogout: () => void;
}

export const Navbar: React.FC<NavbarProps> = ({ user, onLogout }) => {
  const roleName = user.role === 'ADMIN' ? 'Administrator' : 'Finance Officer';

  return (
    <header className="header-bar">
      <div className="brand-section">
        <h1>Billing Service</h1>
        <p>Commercial Fleet Operations &amp; Invoicing Portal</p>
      </div>
      <div className="user-section">
        <div className="user-badge">
          Signed in as <strong>{user.username}</strong>
          <span className={`role-tag ${user.role.toLowerCase()}`}>
            {roleName}
          </span>
        </div>
        <button
          className="btn btn-secondary"
          onClick={onLogout}
          title="Sign out of console"
        >
          Sign Out
        </button>
      </div>
    </header>
  );
};
