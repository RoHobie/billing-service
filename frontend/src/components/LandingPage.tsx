import React, { useState } from 'react';
import { api } from '../services/api';
import { User } from '../types';

interface LandingPageProps {
  onLoginSuccess: (user: User) => void;
}

export const LandingPage: React.FC<LandingPageProps> = ({ onLoginSuccess }) => {
  const [tab, setTab] = useState<'signin' | 'register'>('signin');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [vendorName, setVendorName] = useState('');
  const [contactEmail, setContactEmail] = useState('');
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const fillAdmin = () => {
    setUsername('admin');
    setPassword('admin123');
    setErrorMessage(null);
  };

  const fillFinance = () => {
    setUsername('finance');
    setPassword('finance123');
    setErrorMessage(null);
  };

  const handleSignIn = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!username || !password) {
      setErrorMessage('Please enter both username and password.');
      return;
    }

    setLoading(true);
    setErrorMessage(null);

    try {
      const user = await api.authenticate(username, password);
      onLoginSuccess(user);
    } catch (err: unknown) {
      if (err instanceof Error) {
        setErrorMessage(err.message);
      } else {
        setErrorMessage('Failed to sign in. Please verify your credentials.');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleRegister = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!username || !password) {
      setErrorMessage('Username and password are required.');
      return;
    }

    setLoading(true);
    setErrorMessage(null);

    try {
      const user = await api.registerVendor({
        username,
        password,
        vendorName: vendorName || undefined,
        contactEmail: contactEmail || undefined,
      });
      onLoginSuccess(user);
    } catch (err: unknown) {
      if (err instanceof Error) {
        setErrorMessage(err.message);
      } else {
        setErrorMessage('Registration failed. Please try a different username.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="app-container">
      <div className="landing-hero">
        <h1>Billing Service</h1>
        <p>
          Enterprise commercial fleet invoicing, trip cost auditing, and contract reconciliation platform.
        </p>
      </div>

      <div className="landing-features">
        <div className="feature-box">
          <h3>Fair Cost Allocation</h3>
          <p>Exact paisa reconciliation using mathematical Largest Remainder distribution with zero balance drift.</p>
        </div>
        <div className="feature-box">
          <h3>Contract Rate Versioning</h3>
          <p>Seamless mid-period contract revisions and tiered distance slabs automatically applied to trip logs.</p>
        </div>
        <div className="feature-box">
          <h3>Comprehensive Surcharges</h3>
          <p>Dynamic night allowances, luggage surcharges, and toll reimbursements computed per commercial trip.</p>
        </div>
        <div className="feature-box">
          <h3>Automated Invoicing</h3>
          <p>Official tax invoices generated on demand with complete auditable calculation notes.</p>
        </div>
      </div>

      <div className="auth-card">
        <div className="auth-tabs">
          <button
            type="button"
            className={`auth-tab ${tab === 'signin' ? 'active' : ''}`}
            onClick={() => {
              setTab('signin');
              setErrorMessage(null);
            }}
          >
            Sign In
          </button>
          <button
            type="button"
            className={`auth-tab ${tab === 'register' ? 'active' : ''}`}
            onClick={() => {
              setTab('register');
              setErrorMessage(null);
            }}
          >
            Partner Registration
          </button>
        </div>

        {errorMessage && <div className="error-banner">{errorMessage}</div>}

        {tab === 'signin' ? (
          <div>
            <div className="credential-quickfill">
              <div className="quickfill-label">Pre-fill Credentials</div>
              <div className="quickfill-buttons">
                <button
                  type="button"
                  className="btn btn-fill"
                  onClick={fillAdmin}
                >
                  Admin Credentials
                </button>
                <button
                  type="button"
                  className="btn btn-fill"
                  onClick={fillFinance}
                >
                  Finance Credentials
                </button>
              </div>
            </div>

            <form onSubmit={handleSignIn}>
              <div className="form-group">
                <label className="form-label" htmlFor="username">
                  Username
                </label>
                <input
                  id="username"
                  type="text"
                  className="form-control"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  placeholder="e.g. admin or finance"
                  required
                />
              </div>

              <div className="form-group">
                <label className="form-label" htmlFor="password">
                  Password
                </label>
                <input
                  id="password"
                  type="password"
                  className="form-control"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="Enter password"
                  required
                />
              </div>

              <button
                type="submit"
                className="btn btn-primary"
                style={{ width: '100%', marginTop: '10px' }}
                disabled={loading}
              >
                {loading ? 'Authenticating...' : 'Sign In to Portal'}
              </button>
            </form>
          </div>
        ) : (
          <form onSubmit={handleRegister}>
            <div className="form-group">
              <label className="form-label" htmlFor="reg-vendorName">
                Partner Organization Name
              </label>
              <input
                id="reg-vendorName"
                type="text"
                className="form-control"
                value={vendorName}
                onChange={(e) => setVendorName(e.target.value)}
                placeholder="e.g. Cityline Transit Fleet"
              />
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="reg-email">
                Billing Contact Email
              </label>
              <input
                id="reg-email"
                type="email"
                className="form-control"
                value={contactEmail}
                onChange={(e) => setContactEmail(e.target.value)}
                placeholder="billing@partnerfleet.com"
              />
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="reg-username">
                Account Username
              </label>
              <input
                id="reg-username"
                type="text"
                className="form-control"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                placeholder="Choose username"
                required
              />
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="reg-password">
                Account Password
              </label>
              <input
                id="reg-password"
                type="password"
                className="form-control"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="Choose password"
                required
              />
            </div>

            <div className="notice-box">
              New partner registrations are automatically granted default fleet review access.
            </div>

            <button
              type="submit"
              className="btn btn-primary"
              style={{ width: '100%', marginTop: '16px' }}
              disabled={loading}
            >
              {loading ? 'Creating Account...' : 'Register Partner Account'}
            </button>
          </form>
        )}
      </div>
    </div>
  );
};
