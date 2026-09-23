import React, { useState, useEffect } from 'react';
import { api } from '../services/api';
import { BillLineItem, Page, formatCurrency } from '../types';

interface LineItemsPanelProps {
  runId: number | null;
}

export const LineItemsPanel: React.FC<LineItemsPanelProps> = ({ runId }) => {
  const [pageData, setPageData] = useState<Page<BillLineItem> | null>(null);
  const [currentPage, setCurrentPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [filterTripId, setFilterTripId] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    setCurrentPage(0);
  }, [runId]);

  useEffect(() => {
    if (!runId) {
      setPageData(null);
      return;
    }
    loadItems(runId, currentPage, pageSize);
  }, [runId, currentPage, pageSize]);

  const loadItems = async (id: number, page: number, size: number) => {
    setLoading(true);
    try {
      const data = await api.getBillingRunItems(id, page, size);
      setPageData(data);
    } catch {
      setPageData(null);
    } finally {
      setLoading(false);
    }
  };

  if (!runId) {
    return null;
  }

  const items = pageData?.content || [];
  const filteredItems = filterTripId
    ? items.filter((item) => String(item.tripId).includes(filterTripId.trim()))
    : items;

  return (
    <div className="panel">
      <div className="panel-header">
        <h2 className="panel-title">
          Panel D: Itemised Trip Line Items Breakdown
        </h2>
        <span className="panel-tag">Audited Commercial Ledger</span>
      </div>

      <div className="filter-bar">
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <label className="form-label" style={{ margin: 0 }} htmlFor="filter-trip">
            Filter Trip ID:
          </label>
          <input
            id="filter-trip"
            type="text"
            className="form-control search-input"
            value={filterTripId}
            onChange={(e) => setFilterTripId(e.target.value)}
            placeholder="Search by trip #"
          />
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <label className="form-label" style={{ margin: 0 }} htmlFor="page-size-select">
            Rows per page:
          </label>
          <select
            id="page-size-select"
            className="form-control"
            style={{ width: 'auto' }}
            value={pageSize}
            onChange={(e) => {
              setPageSize(Number(e.target.value));
              setCurrentPage(0);
            }}
          >
            <option value="10">10</option>
            <option value="20">20</option>
            <option value="50">50</option>
          </select>
        </div>
      </div>

      {loading ? (
        <div className="empty-state">Loading itemised trip calculations...</div>
      ) : filteredItems.length === 0 ? (
        <div className="empty-state">No matching trip line items found.</div>
      ) : (
        <div className="table-responsive">
          <table className="data-table">
            <thead>
              <tr>
                <th>Trip ID</th>
                <th className="numeric">Base Fare</th>
                <th className="numeric">Surcharges</th>
                <th className="numeric">Contract Share</th>
                <th className="numeric">Total Invoiced</th>
                <th>Calculation Audit Trail</th>
              </tr>
            </thead>
            <tbody>
              {filteredItems.map((item) => (
                <tr key={item.id}>
                  <td>
                    <strong>Trip #{item.tripId}</strong>
                  </td>
                  <td className="numeric">{formatCurrency(item.basePaisa)}</td>
                  <td className="numeric">{formatCurrency(item.extraChargesPaisa)}</td>
                  <td className="numeric">{formatCurrency(item.fixedFeeSharePaisa)}</td>
                  <td className="numeric" style={{ fontWeight: 700 }}>
                    {formatCurrency(item.totalPaisa)}
                  </td>
                  <td>
                    <span className="audit-note">{item.computationNote}</span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {pageData && (
        <div className="pagination-bar">
          <div className="pagination-info">
            Showing Page <strong>{pageData.number + 1}</strong> of{' '}
            <strong>{Math.max(1, pageData.totalPages)}</strong> ({pageData.totalElements} Total
            Trips Audited)
          </div>

          <div className="pagination-controls">
            <button
              className="btn btn-secondary"
              onClick={() => setCurrentPage((prev) => Math.max(0, prev - 1))}
              disabled={pageData.number === 0 || loading}
            >
              &larr; Prev
            </button>
            <button
              className="btn btn-secondary"
              onClick={() =>
                setCurrentPage((prev) => Math.min(pageData.totalPages - 1, prev + 1))
              }
              disabled={pageData.number >= pageData.totalPages - 1 || loading}
            >
              Next &rarr;
            </button>
          </div>
        </div>
      )}
    </div>
  );
};
