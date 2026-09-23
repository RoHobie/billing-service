import { useState, useEffect } from 'react';
import { User, BillingRunSummary } from './types';
import { getStoredUser, clearAuthCredentials } from './services/api';
import { Navbar } from './components/Navbar';
import { LandingPage } from './components/LandingPage';
import { TelemetryBar } from './components/TelemetryBar';
import { RunBillingPanel } from './components/RunBillingPanel';
import { SummaryPanel } from './components/SummaryPanel';
import { AuditFlagsPanel } from './components/AuditFlagsPanel';
import { LineItemsPanel } from './components/LineItemsPanel';
import './styles.css';

export function App() {
  const [user, setUser] = useState<User | null>(null);
  const [selectedRun, setSelectedRun] = useState<BillingRunSummary | null>(null);
  const [refreshTrigger, setRefreshTrigger] = useState(0);

  useEffect(() => {
    const saved = getStoredUser();
    if (saved) {
      setUser(saved);
    }
  }, []);

  const handleLogout = () => {
    clearAuthCredentials();
    setUser(null);
    setSelectedRun(null);
  };

  const handleRunSelected = (summary: BillingRunSummary) => {
    setSelectedRun(summary);
    setRefreshTrigger((prev) => prev + 1);
  };

  if (!user) {
    return <LandingPage onLoginSuccess={(u) => setUser(u)} />;
  }

  return (
    <div className="app-container">
      <Navbar user={user} onLogout={handleLogout} />

      <TelemetryBar lastUpdated={refreshTrigger} />

      <div className="panels-grid">
        <RunBillingPanel
          user={user}
          onRunSelected={handleRunSelected}
          selectedRunId={selectedRun?.runId}
          refreshTrigger={refreshTrigger}
        />

        <SummaryPanel summary={selectedRun} />
      </div>

      <AuditFlagsPanel runId={selectedRun?.runId ?? null} />

      <LineItemsPanel runId={selectedRun?.runId ?? null} />
    </div>
  );
}

export default App;
