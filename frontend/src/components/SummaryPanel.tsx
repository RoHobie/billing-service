import React, { useState } from 'react';
import { api } from '../services/api';
import { BillingRunSummary, formatCurrency } from '../types';

interface SummaryPanelProps {
  summary: BillingRunSummary | null;
}

export const SummaryPanel: React.FC<SummaryPanelProps> = ({ summary }) => {
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

  if (!summary) {
    return (
      <div className="panel">
        <div className="panel-header">
          <h2 className="panel-title">Panel B: Statement Summary</h2>
          <span className="panel-tag">Financial Ledger</span>
        </div>
        <div className="empty-state">
          No settlement run selected. Please generate or select an existing statement.
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

      <div className="summary-grid">
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
