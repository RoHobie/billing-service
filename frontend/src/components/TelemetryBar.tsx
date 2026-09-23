import React, { useEffect, useState } from 'react';
import { api } from '../services/api';
import { SystemStats, formatCurrency } from '../types';

interface TelemetryBarProps {
  lastUpdated?: number;
}

export const TelemetryBar: React.FC<TelemetryBarProps> = ({ lastUpdated }) => {
  const [stats, setStats] = useState<SystemStats | null>(null);

  const fetchStats = async () => {
    try {
      const data = await api.getSystemStats();
      setStats(data);
    } catch {
      // Ignore background telemetry errors
    }
  };

  useEffect(() => {
    fetchStats();
    const interval = setInterval(fetchStats, 10000);
    return () => clearInterval(interval);
  }, [lastUpdated]);

  return (
    <div className="kpi-bar">
      <div className="kpi-card">
        <div className="kpi-label">Active Fleet Vehicles</div>
        <div className="kpi-value">{stats ? stats.totalVehicles : '--'}</div>
      </div>

      <div className="kpi-card">
        <div className="kpi-label">Commercial Partners</div>
        <div className="kpi-value">{stats ? stats.totalVendors : '--'}</div>
      </div>

      <div className="kpi-card">
        <div className="kpi-label">Settlement Runs</div>
        <div className="kpi-value">{stats ? stats.totalBillingRuns : '--'}</div>
      </div>

      <div className="kpi-card">
        <div className="kpi-label">Total Invoiced Revenue</div>
        <div className="kpi-value">
          {stats ? formatCurrency(stats.totalRevenuePaisa) : '--'}
        </div>
      </div>

      <div className="kpi-card">
        <div className="kpi-label">Audit Discrepancies</div>
        <div className="kpi-value">{stats ? stats.totalFraudFlags : '0'}</div>
      </div>

      <div className="kpi-card">
        <div className="kpi-label">Service Operational Status</div>
        <div className="kpi-value" style={{ fontSize: '1.05rem', display: 'flex', alignItems: 'center' }}>
          <span className="kpi-status-dot"></span>
          Operational
        </div>
      </div>
    </div>
  );
};
