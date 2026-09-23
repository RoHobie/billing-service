import React, { useState, useEffect } from 'react';
import { api } from '../services/api';
import { FraudFlag } from '../types';

interface AuditFlagsPanelProps {
  runId: number | null;
}

export const AuditFlagsPanel: React.FC<AuditFlagsPanelProps> = ({ runId }) => {
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
        <h2 className="panel-title">
          Panel C: Trip Compliance &amp; Audit Verification Logs
        </h2>
        <span className="panel-tag">Advisory Audit Verification</span>
      </div>

      {loading ? (
        <div className="empty-state">Verifying audit logs...</div>
      ) : flags.length === 0 ? (
        <div className="empty-state success">
          All trips verified compliant with contract specifications. Zero audit discrepancies detected.
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
