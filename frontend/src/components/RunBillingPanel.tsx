import React, { useState, useEffect } from 'react';
import { api } from '../services/api';
import { User, Vehicle, Vendor, BillingRunSummary } from '../types';

interface RunBillingPanelProps {
  user: User;
  selectedVehicle: Vehicle | null;
  onVehicleSelected: (vehicle: Vehicle) => void;
  selectedRun: BillingRunSummary | null;
  onRunSelected: (summary: BillingRunSummary | null) => void;
  refreshTrigger?: number;
}

export const RunBillingPanel: React.FC<RunBillingPanelProps> = ({
  user,
  selectedVehicle,
  onVehicleSelected,
  selectedRun,
  onRunSelected,
  refreshTrigger,
}) => {
  const [vendors, setVendors] = useState<Vendor[]>([]);
  const [selectedVendorId, setSelectedVendorId] = useState<number | ''>('');
  const [vehicles, setVehicles] = useState<Vehicle[]>([]);
  const [vehicleRuns, setVehicleRuns] = useState<BillingRunSummary[]>([]);
  const [billingMonth, setBillingMonth] = useState('2026-01');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const isAdmin = user.role === 'ADMIN';

  // Load vendors list on mount
  useEffect(() => {
    api.getVendors().then(setVendors).catch(() => {});
  }, []);

  // Load vehicles when vendor filter changes or on refresh
  useEffect(() => {
    let isCancelled = false;
    const loadVehicles = async () => {
      try {
        const vList = selectedVendorId === ''
          ? await api.getVehicles()
          : await api.getVehiclesByVendor(Number(selectedVendorId));
        if (!isCancelled) {
          setVehicles(vList);
          // If no vehicle is selected, or currently selected vehicle is not in this filtered list
          if (vList.length > 0) {
            const stillPresent = selectedVehicle && vList.some((v) => v.id === selectedVehicle.id);
            if (!stillPresent) {
              onVehicleSelected(vList[0]);
            }
          }
        }
      } catch {
        if (!isCancelled) {
          setError('Failed to load fleet vehicles');
        }
      }
    };
    loadVehicles();
    return () => {
      isCancelled = true;
    };
  }, [selectedVendorId, refreshTrigger]);

  // Load runs specifically for the selected vehicle
  useEffect(() => {
    let isCancelled = false;
    if (!selectedVehicle) {
      setVehicleRuns([]);
      onRunSelected(null);
      return;
    }

    const loadVehicleRuns = async () => {
      try {
        const runs = await api.getBillingRuns(selectedVehicle.id);
        if (!isCancelled) {
          setVehicleRuns(runs);
          if (runs.length > 0) {
            // Keep current run if it belongs to this vehicle, otherwise select the latest run
            const currentBelongs = selectedRun && runs.some((r) => r.runId === selectedRun.runId);
            if (!currentBelongs) {
              onRunSelected(runs[0]);
            }
          } else {
            onRunSelected(null);
          }
        }
      } catch {
        if (!isCancelled) {
          setVehicleRuns([]);
          onRunSelected(null);
        }
      }
    };

    loadVehicleRuns();
    return () => {
      isCancelled = true;
    };
  }, [selectedVehicle?.id, refreshTrigger]);

  const handleExecuteRun = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedVehicle) return;

    setLoading(true);
    setError(null);

    try {
      const summary = await api.runBilling(selectedVehicle.id, billingMonth);
      onRunSelected(summary);
      // Refresh runs for this vehicle
      const updatedRuns = await api.getBillingRuns(selectedVehicle.id);
      setVehicleRuns(updatedRuns);
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

  const handleVehicleChange = (vehicleId: number) => {
    const found = vehicles.find((v) => v.id === vehicleId);
    if (found) {
      onVehicleSelected(found);
    }
  };

  const handleSelectPastRun = (runIdStr: string) => {
    const id = Number(runIdStr);
    const found = vehicleRuns.find((r) => r.runId === id);
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

      <div className="form-group">
        <label className="form-label" htmlFor="vendor-filter">
          Filter by Logistics Vendor (Optional)
        </label>
        <select
          id="vendor-filter"
          className="form-control"
          value={selectedVendorId}
          onChange={(e) => setSelectedVendorId(e.target.value === '' ? '' : Number(e.target.value))}
        >
          <option value="">All Registered Vendors ({vendors.length})</option>
          {vendors.map((v) => (
            <option key={v.id} value={v.id}>
              {v.name}
            </option>
          ))}
        </select>
        <div className="form-note">
          Filters vehicles under the selected vendor using the vendor vehicle API.
        </div>
      </div>

      <form onSubmit={handleExecuteRun}>
        <div className="form-group">
          <label className="form-label" htmlFor="vehicle-select">
            Select Commercial Fleet Vehicle
          </label>
          <select
            id="vehicle-select"
            className="form-control"
            value={selectedVehicle?.id || ''}
            onChange={(e) => handleVehicleChange(Number(e.target.value))}
          >
            {vehicles.map((v) => (
              <option key={v.id} value={v.id}>
                {v.registrationNumber} — {v.vehicleType || v.type} ({v.vendorName})
              </option>
            ))}
          </select>
          {selectedVehicle && (
            <div className="vehicle-selected-chip">
              Active Vehicle: <strong>{selectedVehicle.registrationNumber}</strong> | Vendor: <strong>{selectedVehicle.vendorName}</strong>
            </div>
          )}
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
          disabled={loading || !isAdmin || !selectedVehicle}
        >
          {loading ? 'Processing Settlement...' : `Generate Settlement Statement for ${selectedVehicle?.registrationNumber || 'Vehicle'}`}
        </button>

        {!isAdmin && (
          <div className="notice-box">
            Statement generation is restricted to Administrator role. You may inspect all existing statements below.
          </div>
        )}
      </form>

      <div style={{ marginTop: '20px', paddingTop: '16px', borderTop: '1px solid var(--border-color)' }}>
        <label className="form-label" htmlFor="historical-runs">
          Statements for {selectedVehicle?.registrationNumber || 'Selected Vehicle'}
        </label>
        {vehicleRuns.length > 0 ? (
          <select
            id="historical-runs"
            className="form-control"
            value={selectedRun?.runId || ''}
            onChange={(e) => handleSelectPastRun(e.target.value)}
          >
            {vehicleRuns.map((r) => (
              <option key={r.runId} value={r.runId}>
                Run #{r.runId} — Period: {r.billingMonth} ({r.lineItemCount} Trips Audited)
              </option>
            ))}
          </select>
        ) : (
          <div className="notice-box" style={{ marginTop: 0, fontStyle: 'italic' }}>
            No statements generated yet for vehicle {selectedVehicle?.registrationNumber}. Click &quot;Generate Settlement Statement&quot; above to run settlement.
          </div>
        )}
      </div>
    </div>
  );
};
