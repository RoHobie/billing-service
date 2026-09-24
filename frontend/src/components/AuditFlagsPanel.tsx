import React, { useState, useEffect } from 'react';
import { api } from '../services/api';
import { FraudFlag, Vehicle } from '../types';

interface AuditFlagsPanelProps {
  runId: number | null;
  vehicle: Vehicle | null;
}

export const AuditFlagsPanel: React.FC<AuditFlagsPanelProps> = ({ runId, vehicle }) => {
  const [flags, setFlags] = useState<FraudFlag[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!runId) {
      setFlags([]);
      return;
    }
    loadFlags(runId);
  }, [runId]);

  const loadFlags = async (id: number) => {
    setLoading(true);
    try {
      const data = await api.getBillingRunFlags(id);
      setFlags(data);
    } catch {
      setFlags([]);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="panel">
      <div className="panel-header">
        <div>
          <h2 className="panel-title">
            Panel C: Trip Compliance &amp; Audit Verification Logs
          </h2>
          <div style={{ fontSize: '0.8125rem', color: 'var(--text-muted)', marginTop: '2px' }}>
            Vehicle: <strong>{vehicle?.registrationNumber || 'N/A'}</strong> {runId ? `(Run #${runId})` : ''}
          </div>
        </div>
        <span className="panel-tag">Advisory Audit Verification</span>
      </div>

      {loading ? (
        <div className="empty-state">Verifying audit logs...</div>
      ) : !runId ? (
        <div className="empty-state">
          No settlement statement selected. Select or generate a statement in Panel A to inspect compliance audit logs.
        </div>
      ) : flags.length === 0 ? (
        <div className="empty-state success">
          All audited trips for vehicle <strong>{vehicle?.registrationNumber}</strong> verified compliant with contract specifications. Zero audit discrepancies detected.
        </div>
      ) : (
        <div className="table-responsive">
          <table className="data-table">
            <thead>
              <tr>
                <th>Notice Type</th>
                <th>Trip Reference</th>
                <th>Audit Finding / Finding Details</th>
              </tr>
            </thead>
            <tbody>
              {flags.map((flag) => (
                <tr key={flag.id}>
                  <td>
                    <span className="badge badge-warning">{flag.flagType}</span>
                  </td>
                  <td>Trip #{flag.tripId}</td>
                  <td>{flag.description}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
};
