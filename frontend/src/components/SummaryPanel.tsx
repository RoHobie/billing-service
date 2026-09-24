import React, { useState } from 'react';
import { api } from '../services/api';
import { Vehicle, BillingRunSummary, formatCurrency } from '../types';

interface SummaryPanelProps {
  vehicle: Vehicle | null;
  summary: BillingRunSummary | null;
}

export const SummaryPanel: React.FC<SummaryPanelProps> = ({ vehicle, summary }) => {
  const [downloading, setDownloading] = useState(false);
  const [downloadError, setDownloadError] = useState<string | null>(null);

  const handleDownloadPdf = async () => {
    if (!summary) return;
    setDownloading(true);
    setDownloadError(null);
    try {
      await api.downloadInvoicePdf(summary.runId);
    } catch (err: unknown) {
      if (err instanceof Error) {
        setDownloadError(err.message);
      } else {
        setDownloadError('Could not download PDF invoice.');
      }
    } finally {
      setDownloading(false);
    }
  };

  if (!vehicle) {
    return (
      <div className="panel">
        <div className="panel-header">
          <h2 className="panel-title">Panel B: Statement Summary</h2>
          <span className="panel-tag">Financial Ledger</span>
        </div>
        <div className="empty-state">
          No fleet vehicle selected. Please select a vehicle from Panel A.
        </div>
      </div>
    );
  }

  if (!summary) {
    return (
      <div className="panel">
        <div className="panel-header">
          <h2 className="panel-title">Panel B: Statement Summary</h2>
          <span className="panel-tag">Financial Ledger</span>
        </div>
        <div className="vehicle-banner">
          <div>
            <div className="vehicle-banner-title">{vehicle.registrationNumber}</div>
            <div className="vehicle-banner-sub">
              {vehicle.vehicleType || vehicle.type || 'Commercial Fleet'} &bull; {vehicle.vendorName}
            </div>
          </div>
          <span className="badge badge-warning">Unsettled</span>
        </div>
        <div className="empty-state" style={{ marginTop: '16px' }}>
          No settlement statement generated yet for vehicle <strong>{vehicle.registrationNumber}</strong>.
          <p style={{ marginTop: '6px', fontSize: '0.8125rem' }}>
            Click <em>&quot;Generate Settlement Statement&quot;</em> in Panel A to compute base slabs, mid-month contract changes, surcharges, and fixed fee splits.
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="panel">
      <div className="panel-header">
        <h2 className="panel-title">Panel B: Statement Summary</h2>
        <span className="badge badge-success">{summary.status}</span>
      </div>

      {downloadError && <div className="error-banner">{downloadError}</div>}

      <div className="vehicle-banner">
        <div>
          <div className="vehicle-banner-title">{vehicle.registrationNumber}</div>
          <div className="vehicle-banner-sub">
            {vehicle.vehicleType || vehicle.type || 'Commercial Fleet'} &bull; {vehicle.vendorName}
          </div>
        </div>
        <span className="badge badge-success">Audited</span>
      </div>

      <div className="summary-grid" style={{ marginTop: '14px' }}>
        <div className="summary-item">
          <div className="summary-item-label">Statement Reference</div>
          <div className="summary-item-value">Run #{summary.runId}</div>
        </div>

        <div className="summary-item">
          <div className="summary-item-label">Billing Period</div>
          <div className="summary-item-value">{summary.billingMonth}</div>
        </div>

        <div className="summary-item">
          <div className="summary-item-label">Audited Trips</div>
          <div className="summary-item-value">{summary.lineItemCount} Trips</div>
        </div>

        <div className="summary-item">
          <div className="summary-item-label">Settlement Grand Total</div>
          <div className="summary-item-value highlight">
            {formatCurrency(summary.grandTotalPaisa)}
          </div>
        </div>
      </div>

      <button
        type="button"
        className="btn btn-secondary"
        style={{ width: '100%' }}
        onClick={handleDownloadPdf}
        disabled={downloading}
      >
        {downloading ? 'Preparing PDF Document...' : 'Download Official Tax Invoice (PDF)'}
      </button>
    </div>
  );
};
