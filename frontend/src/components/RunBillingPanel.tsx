import React, { useState, useEffect } from 'react';
import { api } from '../services/api';
import { User, Vehicle, BillingRunSummary } from '../types';

interface RunBillingPanelProps {
  user: User;
  onRunSelected: (summary: BillingRunSummary) => void;
  selectedRunId?: number;
  refreshTrigger?: number;
}

export const RunBillingPanel: React.FC<RunBillingPanelProps> = ({
  user,
  onRunSelected,
  selectedRunId,
  refreshTrigger,
}) => {
  const [vehicles, setVehicles] = useState<Vehicle[]>([]);
  const [runs, setRuns] = useState<BillingRunSummary[]>([]);
  const [selectedVehicleId, setSelectedVehicleId] = useState<number | ''>('');
  const [billingMonth, setBillingMonth] = useState('2026-01');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const isAdmin = user.role === 'ADMIN';

  useEffect(() => {
    loadData();
  }, [refreshTrigger]);

  const loadData = async () => {
    try {
      const [vList, rList] = await Promise.all([
        api.getVehicles(),
        api.getBillingRuns(),
      ]);
      setVehicles(vList);
      setRuns(rList);
      if (vList.length > 0 && selectedVehicleId === '') {
        setSelectedVehicleId(vList[0].id);
      }
      if (rList.length > 0 && !selectedRunId) {
        onRunSelected(rList[0]);
      }
    } catch {
      // Ignore initial load failure
    }
  };

  const handleExecuteRun = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedVehicleId) return;

    setLoading(true);
    setError(null);

    try {
      const summary = await api.runBilling(Number(selectedVehicleId), billingMonth);
      onRunSelected(summary);
      await loadData();
    } catch (err: unknown) {
      if (err instanceof Error) {
        setError(err.message);
      } else {
        setError('Failed to execute settlement run.');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleSelectPastRun = (runIdStr: string) => {
    const id = Number(runIdStr);
    const found = runs.find((r) => r.runId === id);
    if (found) {
      onRunSelected(found);
    }
  };

  return (
    <div className="panel">
      <div className="panel-header">
        <h2 className="panel-title">
          Panel A: Settlement Run &amp; Vehicle Selection
        </h2>
        <span className="panel-tag">Operations Console</span>
      </div>

      {error && <div className="error-banner">{error}</div>}

      <form onSubmit={handleExecuteRun}>
        <div className="form-group">
          <label className="form-label" htmlFor="vehicle-select">
            Select Commercial Fleet Vehicle
          </label>
          <select
            id="vehicle-select"
            className="form-control"
            value={selectedVehicleId}
            onChange={(e) => setSelectedVehicleId(Number(e.target.value))}
          >
            {vehicles.map((v) => (
              <option key={v.id} value={v.id}>
                {v.registrationNumber} — {v.vehicleType} ({v.vendorName})
              </option>
            ))}
          </select>
        </div>

        <div className="form-group">
          <label className="form-label" htmlFor="billing-month">
            Billing Settlement Month (YYYY-MM)
          </label>
          <input
            id="billing-month"
            type="text"
            className="form-control"
            value={billingMonth}
            onChange={(e) => setBillingMonth(e.target.value)}
            placeholder="2026-01"
            pattern="[0-9]{4}-[0-9]{2}"
            title="Format: YYYY-MM"
            required
          />
          <div className="form-note">
            Calculates base slabs, mid-month contract changes, surcharges, and fixed fee splits.
          </div>
        </div>

        <button
          type="submit"
          className="btn btn-primary"
          style={{ width: '100%', marginTop: '6px' }}
          disabled={loading || !isAdmin || !selectedVehicleId}
        >
          {loading ? 'Processing Settlement...' : 'Generate Settlement Statement'}
        </button>

        {!isAdmin && (
          <div className="notice-box">
            Statement generation is restricted to Administrator role. You may inspect all existing statements below.
          </div>
        )}
      </form>

      {runs.length > 0 && (
        <div style={{ marginTop: '20px', paddingTop: '16px', borderTop: '1px solid var(--border-color)' }}>
          <label className="form-label" htmlFor="historical-runs">
            Inspect Generated Statement
          </label>
          <select
            id="historical-runs"
            className="form-control"
            value={selectedRunId || ''}
            onChange={(e) => handleSelectPastRun(e.target.value)}
          >
            {runs.map((r) => {
              const matchedVeh = vehicles.find((v) => v.id === r.vehicleId);
              const label = matchedVeh
                ? `${matchedVeh.registrationNumber} (${r.billingMonth}) — Run #${r.runId}`
                : `Vehicle #${r.vehicleId} (${r.billingMonth}) — Run #${r.runId}`;
              return (
                <option key={r.runId} value={r.runId}>
                  {label}
                </option>
              );
            })}
          </select>
        </div>
      )}
    </div>
  );
};
