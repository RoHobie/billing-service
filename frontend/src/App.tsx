import { useState, useEffect } from 'react';
import { User, Vehicle, BillingRunSummary } from './types';
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
  const [selectedVehicle, setSelectedVehicle] = useState<Vehicle | null>(null);
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
    setSelectedVehicle(null);
    setSelectedRun(null);
  };

  const handleRunSelected = (summary: BillingRunSummary | null) => {
    setSelectedRun(summary);
    setRefreshTrigger((prev) => prev + 1);
  };

  const handleVehicleSelected = (vehicle: Vehicle) => {
    setSelectedVehicle(vehicle);
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
          selectedVehicle={selectedVehicle}
          onVehicleSelected={handleVehicleSelected}
          selectedRun={selectedRun}
          onRunSelected={handleRunSelected}
          refreshTrigger={refreshTrigger}
        />

        <SummaryPanel
          vehicle={selectedVehicle}
          summary={selectedRun}
        />
      </div>

      <AuditFlagsPanel
        runId={selectedRun?.runId ?? null}
        vehicle={selectedVehicle}
      />

      <LineItemsPanel
        runId={selectedRun?.runId ?? null}
        vehicle={selectedVehicle}
        billingMonth={selectedRun?.billingMonth}
      />
    </div>
  );
}

export default App;
