import React, { useState, useEffect } from 'react';
import { api } from '../services/api';
import { BillLineItem, TripRecord, Page, Vehicle, formatCurrency } from '../types';

interface LineItemsPanelProps {
  runId: number | null;
  vehicle: Vehicle | null;
  billingMonth?: string;
}

export const LineItemsPanel: React.FC<LineItemsPanelProps> = ({ runId, vehicle, billingMonth }) => {
  const [activeTab, setActiveTab] = useState<'audited' | 'loggedTrips'>('audited');
  
  // Audited line items state
  const [pageData, setPageData] = useState<Page<BillLineItem> | null>(null);
  const [currentPage, setCurrentPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const [filterTripId, setFilterTripId] = useState('');
  const [loadingItems, setLoadingItems] = useState(false);

  // Raw logged trips state
  const [vehicleTrips, setVehicleTrips] = useState<TripRecord[]>([]);
  const [loadingTrips, setLoadingTrips] = useState(false);
  const [tripFilterId, setTripFilterId] = useState('');
  const [tripPage, setTripPage] = useState(0);
  const [tripPageSize, setTripPageSize] = useState(10);

  // Reset page when runId or vehicle changes
  useEffect(() => {
    setCurrentPage(0);
  }, [runId]);

  useEffect(() => {
    setTripPage(0);
  }, [vehicle?.id]);

  // Load audited line items when runId, page, or pageSize changes
  useEffect(() => {
    if (!runId) {
      setPageData(null);
      return;
    }
    loadItems(runId, currentPage, pageSize);
  }, [runId, currentPage, pageSize]);

  // Load raw logged trips when vehicle changes
  useEffect(() => {
    if (!vehicle) {
      setVehicleTrips([]);
      return;
    }
    loadVehicleTrips(vehicle.id);
  }, [vehicle?.id]);

  const loadItems = async (id: number, page: number, size: number) => {
    setLoadingItems(true);
    try {
      const data = await api.getBillingRunItems(id, page, size);
      setPageData(data);
    } catch {
      setPageData(null);
    } finally {
      setLoadingItems(false);
    }
  };

  const loadVehicleTrips = async (vehicleId: number) => {
    setLoadingTrips(true);
    try {
      const trips = await api.getTripsByVehicle(vehicleId);
      setVehicleTrips(trips);
    } catch {
      setVehicleTrips([]);
    } finally {
      setLoadingTrips(false);
    }
  };

  const formatTimestamp = (ts?: string) => {
    if (!ts) return '—';
    try {
      const date = new Date(ts);
      return isNaN(date.getTime()) ? ts : date.toLocaleString('en-IN', {
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      });
    } catch {
      return ts;
    }
  };

  // Render Audited Line Items Content
  const renderAuditedItems = () => {
    if (!runId) {
      return (
        <div className="empty-state">
          No settlement statement selected for vehicle{' '}
          <strong>{vehicle?.registrationNumber || 'Selected Vehicle'}</strong>.
          <p style={{ marginTop: '6px', fontSize: '0.8125rem' }}>
            Generate a settlement run in Panel A or switch to the &quot;Logged Vehicle Trips&quot; tab to inspect trip telemetry.
          </p>
        </div>
      );
    }

    const items = pageData?.content || [];
    const filteredItems = filterTripId
      ? items.filter((item) => String(item.tripId).includes(filterTripId.trim()))
      : items;

    const totalPages = pageData ? Math.max(1, pageData.totalPages) : 1;
    const pageNum = pageData ? pageData.number : 0;
    const totalElements = pageData ? pageData.totalElements : 0;
    const startIdx = totalElements > 0 ? pageNum * pageSize + 1 : 0;
    const endIdx = Math.min((pageNum + 1) * pageSize, totalElements);

    return (
      <>
        <div className="filter-bar">
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flexWrap: 'wrap' }}>
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
            {filterTripId && (
              <button
                type="button"
                className="btn btn-fill"
                onClick={() => setFilterTripId('')}
                style={{ padding: '4px 8px', fontSize: '0.75rem' }}
              >
                Clear
              </button>
            )}
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
              <option value="5">5</option>
              <option value="10">10</option>
              <option value="20">20</option>
              <option value="50">50</option>
            </select>
          </div>
        </div>

        {loadingItems ? (
          <div className="empty-state">Loading itemised trip calculations...</div>
        ) : filteredItems.length === 0 ? (
          <div className="empty-state">
            {filterTripId ? `No trips found matching "#${filterTripId}".` : 'No audited trip line items found for this run.'}
          </div>
        ) : (
          <div className="table-responsive">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Trip ID</th>
                  <th>Trip Date / Time</th>
                  <th className="numeric">Distance</th>
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
                    <td>{formatTimestamp(item.startTime)}</td>
                    <td className="numeric">{item.distanceKm != null ? `${item.distanceKm} km` : '—'}</td>
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
              Showing trips <strong>{startIdx} &ndash; {endIdx}</strong> of{' '}
              <strong>{totalElements}</strong> audited trips for vehicle <strong>{vehicle?.registrationNumber}</strong>
            </div>

            <div className="pagination-controls">
              <button
                className="btn btn-secondary"
                onClick={() => setCurrentPage((prev) => Math.max(0, prev - 1))}
                disabled={pageNum === 0 || loadingItems}
              >
                &larr; Prev
              </button>

              <div className="pagination-pages">
                {Array.from({ length: totalPages }, (_, i) => (
                  <button
                    key={i}
                    className={`page-num-btn ${i === pageNum ? 'active' : ''}`}
                    onClick={() => setCurrentPage(i)}
                    disabled={loadingItems}
                  >
                    {i + 1}
                  </button>
                ))}
              </div>

              <button
                className="btn btn-secondary"
                onClick={() => setCurrentPage((prev) => Math.min(totalPages - 1, prev + 1))}
                disabled={pageNum >= totalPages - 1 || loadingItems}
              >
                Next &rarr;
              </button>
            </div>
          </div>
        )}
      </>
    );
  };

  // Render Logged Raw Trips Content
  const renderLoggedTrips = () => {
    if (!vehicle) {
      return <div className="empty-state">No fleet vehicle selected.</div>;
    }

    const filtered = tripFilterId
      ? vehicleTrips.filter((t) => String(t.id).includes(tripFilterId.trim()))
      : vehicleTrips;

    const totalPages = Math.max(1, Math.ceil(filtered.length / tripPageSize));
    const startIdx = filtered.length > 0 ? tripPage * tripPageSize + 1 : 0;
    const endIdx = Math.min((tripPage + 1) * tripPageSize, filtered.length);
    const paginatedTrips = filtered.slice(tripPage * tripPageSize, (tripPage + 1) * tripPageSize);

    return (
      <>
        <div className="filter-bar">
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flexWrap: 'wrap' }}>
            <label className="form-label" style={{ margin: 0 }} htmlFor="filter-raw-trip">
              Filter Trip ID:
            </label>
            <input
              id="filter-raw-trip"
              type="text"
              className="form-control search-input"
              value={tripFilterId}
              onChange={(e) => {
                setTripFilterId(e.target.value);
                setTripPage(0);
              }}
              placeholder="Search by trip #"
            />
            {tripFilterId && (
              <button
                type="button"
                className="btn btn-fill"
                onClick={() => setTripFilterId('')}
                style={{ padding: '4px 8px', fontSize: '0.75rem' }}
              >
                Clear
              </button>
            )}
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <label className="form-label" style={{ margin: 0 }} htmlFor="trip-page-size-select">
              Rows per page:
            </label>
            <select
              id="trip-page-size-select"
              className="form-control"
              style={{ width: 'auto' }}
              value={tripPageSize}
              onChange={(e) => {
                setTripPageSize(Number(e.target.value));
                setTripPage(0);
              }}
            >
              <option value="5">5</option>
              <option value="10">10</option>
              <option value="20">20</option>
            </select>
          </div>
        </div>

        {loadingTrips ? (
          <div className="empty-state">Loading recorded vehicle trips...</div>
        ) : paginatedTrips.length === 0 ? (
          <div className="empty-state">
            {tripFilterId ? `No trips found matching "#${tripFilterId}".` : `No recorded trips found for vehicle ${vehicle.registrationNumber}.`}
          </div>
        ) : (
          <div className="table-responsive">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Trip ID</th>
                  <th>Start Time</th>
                  <th>End Time</th>
                  <th className="numeric">Distance</th>
                  <th>Dead Leg</th>
                  <th>Night Charge</th>
                  <th className="numeric">Waiting Mins</th>
                  <th className="numeric">Tolls (Paisa)</th>
                </tr>
              </thead>
              <tbody>
                {paginatedTrips.map((trip) => (
                  <tr key={trip.id}>
                    <td>
                      <strong>Trip #{trip.id}</strong>
                    </td>
                    <td>{formatTimestamp(trip.startTime)}</td>
                    <td>{formatTimestamp(trip.endTime)}</td>
                    <td className="numeric">{trip.distanceKm} km</td>
                    <td>
                      {trip.isDeadLeg ? (
                        <span className="badge badge-warning">Dead Leg</span>
                      ) : (
                        <span style={{ color: 'var(--text-dim)' }}>No</span>
                      )}
                    </td>
                    <td>
                      {trip.hasNightCharge ? (
                        <span className="badge badge-warning">Night Rate</span>
                      ) : (
                        <span style={{ color: 'var(--text-dim)' }}>Standard</span>
                      )}
                    </td>
                    <td className="numeric">{trip.waitingMinutes > 0 ? `${trip.waitingMinutes} min` : '0'}</td>
                    <td className="numeric">{formatCurrency(trip.tollAmountPaisa)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {filtered.length > 0 && (
          <div className="pagination-bar">
            <div className="pagination-info">
              Showing trips <strong>{startIdx} &ndash; {endIdx}</strong> of{' '}
              <strong>{filtered.length}</strong> logged trips for vehicle <strong>{vehicle.registrationNumber}</strong>
            </div>

            <div className="pagination-controls">
              <button
                className="btn btn-secondary"
                onClick={() => setTripPage((prev) => Math.max(0, prev - 1))}
                disabled={tripPage === 0 || loadingTrips}
              >
                &larr; Prev
              </button>

              <div className="pagination-pages">
                {Array.from({ length: totalPages }, (_, i) => (
                  <button
                    key={i}
                    className={`page-num-btn ${i === tripPage ? 'active' : ''}`}
                    onClick={() => setTripPage(i)}
                    disabled={loadingTrips}
                  >
                    {i + 1}
                  </button>
                ))}
              </div>

              <button
                className="btn btn-secondary"
                onClick={() => setTripPage((prev) => Math.min(totalPages - 1, prev + 1))}
                disabled={tripPage >= totalPages - 1 || loadingTrips}
              >
                Next &rarr;
              </button>
            </div>
          </div>
        )}
      </>
    );
  };

  return (
    <div className="panel">
      <div className="panel-header">
        <div>
          <h2 className="panel-title">
            Panel D: Itemised Trip Line Items
          </h2>
          <div style={{ fontSize: '0.8125rem', color: 'var(--text-muted)', marginTop: '2px' }}>
            Fleet Vehicle: <strong>{vehicle?.registrationNumber || 'N/A'}</strong> &bull; Vendor: <strong>{vehicle?.vendorName || 'N/A'}</strong> {billingMonth && <>&bull; Period: <strong>{billingMonth}</strong></>}
          </div>
        </div>

        <div className="panel-tabs">
          <button
            type="button"
            className={`tab-btn ${activeTab === 'audited' ? 'active' : ''}`}
            onClick={() => setActiveTab('audited')}
          >
            Audited Statement Items {runId ? `(Run #${runId})` : ''}
          </button>
          <button
            type="button"
            className={`tab-btn ${activeTab === 'loggedTrips' ? 'active' : ''}`}
            onClick={() => setActiveTab('loggedTrips')}
          >
            All Logged Vehicle Trips ({vehicleTrips.length})
          </button>
        </div>
      </div>

      {activeTab === 'audited' ? renderAuditedItems() : renderLoggedTrips()}
    </div>
  );
};
