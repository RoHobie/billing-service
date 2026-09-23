/**
 * FastFleet Operations Portal — Vanilla JavaScript Client
 * Minimal, framework-free interactive operations console.
 */

// State
let currentRunDetails = null;

// DOM Elements
const authSelect = document.getElementById('auth-role-select');
const alertBanner = document.getElementById('alert-banner');
const itemsModal = document.getElementById('items-modal');

// --- Helper Functions ---

function getAuthHeader() {
  const credentials = authSelect ? authSelect.value : 'admin:admin123';
  return 'Basic ' + btoa(credentials);
}

async function apiFetch(endpoint, options = {}) {
  const headers = {
    'Accept': 'application/json',
    'Authorization': getAuthHeader(),
    ...(options.headers || {})
  };

  const response = await fetch(endpoint, { ...options, headers });

  if (response.status === 401) {
    showAlert('Authentication required or invalid credentials.', 'error');
    throw new Error('Unauthorized');
  }

  if (response.status === 403) {
    showAlert('Access denied: insufficient permissions for this operation.', 'error');
    throw new Error('Forbidden');
  }

  return response;
}

function showAlert(message, type = 'error') {
  if (!alertBanner) return;
  alertBanner.className = `alert-banner ${type}`;
  alertBanner.textContent = message;
  alertBanner.classList.remove('hidden');
  setTimeout(() => {
    alertBanner.classList.add('hidden');
  }, 5000);
}

function formatPaisa(paisa) {
  if (paisa === null || paisa === undefined) return '₹ 0.00';
  const rupees = Number(paisa) / 100;
  return '₹ ' + rupees.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function formatUptime(seconds) {
  if (!seconds) return '0m';
  const hrs = Math.floor(seconds / 3600);
  const mins = Math.floor((seconds % 3600) / 60);
  return hrs > 0 ? `${hrs}h ${mins}m` : `${mins}m`;
}

// --- Navigation Tabs ---

function initTabs() {
  const tabButtons = document.querySelectorAll('.tab-btn');
  tabButtons.forEach(btn => {
    btn.addEventListener('click', () => {
      const targetId = btn.getAttribute('data-tab');
      switchToTab(targetId);
    });
  });
}

function switchToTab(tabId) {
  document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
  document.querySelectorAll('.tab-pane').forEach(p => p.classList.remove('active'));

  const btn = document.querySelector(`.tab-btn[data-tab="${tabId}"]`);
  const pane = document.getElementById(tabId);

  if (btn) btn.classList.add('active');
  if (pane) pane.classList.add('active');

  // Trigger lazy loading for tab
  if (tabId === 'tab-overview') loadTelemetry();
  if (tabId === 'tab-billing' || tabId === 'tab-fleet') loadVehicles();
}

// --- Tab 1: Operational Overview ---

async function loadTelemetry() {
  try {
    const res = await apiFetch('/api/monitoring/stats');
    if (!res.ok) throw new Error('Failed to load telemetry stats');
    const body = await res.json();
    const data = body.data;

    document.getElementById('kpi-vehicles').textContent = data.totalVehicles;
    document.getElementById('kpi-vendors').textContent = data.totalVendors;
    document.getElementById('kpi-revenue').textContent = formatPaisa(data.totalRevenuePaisa);
    document.getElementById('kpi-runs').textContent = data.totalBillingRuns;
    document.getElementById('kpi-flags').textContent = data.totalFraudFlags;
    document.getElementById('kpi-uptime').textContent = formatUptime(data.uptimeSeconds);

    const statusBadge = document.getElementById('system-status-badge');
    const statusText = document.getElementById('system-status-text');
    if (data.systemHealth === 'OPERATIONAL') {
      statusBadge.className = 'status-badge';
      statusText.textContent = 'Operational';
    }
  } catch (err) {
    console.error('Error fetching telemetry:', err);
  }
}

// --- Tab 2: Run Settlement ---

async function loadVehicles() {
  const select = document.getElementById('select-vehicle');
  const tableBody = document.querySelector('#table-vehicles tbody');

  try {
    const res = await apiFetch('/api/vehicles');
    if (!res.ok) throw new Error('Failed to retrieve vehicle roster');
    const body = await res.json();
    const vehicles = body.data || [];

    if (select) {
      select.innerHTML = '<option value="">-- Choose Vehicle Asset --</option>';
      vehicles.forEach(v => {
        const vendorName = v.vendor ? v.vendor.name : 'Unknown Partner';
        const opt = document.createElement('option');
        opt.value = v.id;
        opt.textContent = `${v.registrationNumber} (${v.type}) — ${vendorName}`;
        select.appendChild(opt);
      });
    }

    if (tableBody) {
      if (vehicles.length === 0) {
        tableBody.innerHTML = '<tr><td colspan="5" class="text-center text-muted">No vehicles registered</td></tr>';
      } else {
        tableBody.innerHTML = vehicles.map(v => `
          <tr>
            <td>#${v.id}</td>
            <td><strong>${v.registrationNumber}</strong></td>
            <td>${v.type}</td>
            <td>${v.vendor ? v.vendor.name : '-'}</td>
            <td>
              <button class="btn btn-secondary btn-sm" onclick="selectAndGoToSettlement(${v.id})">Settle Vehicle</button>
            </td>
          </tr>
        `).join('');
      }
    }
  } catch (err) {
    console.error('Error loading vehicles:', err);
  }
}

function selectAndGoToSettlement(vehicleId) {
  switchToTab('tab-billing');
  const select = document.getElementById('select-vehicle');
  if (select) select.value = vehicleId;
}

async function handleBillingSubmit(e) {
  e.preventDefault();
  const vehicleId = document.getElementById('select-vehicle').value;
  const billingMonth = document.getElementById('input-billing-month').value.trim();
  const submitBtn = document.getElementById('btn-submit-billing');

  if (!vehicleId || !billingMonth) {
    showAlert('Please select a vehicle and billing month.', 'error');
    return;
  }

  submitBtn.disabled = true;
  submitBtn.textContent = 'Computing Settlement...';

  try {
    const res = await apiFetch('/api/billing/run', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ vehicleId: Number(vehicleId), billingMonth })
    });

    const body = await res.json();
    if (!res.ok) {
      showAlert(body.message || 'Settlement run execution failed.', 'error');
      return;
    }

    const summary = body.data;
    showBillingResult(summary);
    showAlert('Settlement calculation completed successfully.', 'success');
    loadTelemetry();
  } catch (err) {
    console.error('Billing run error:', err);
  } finally {
    submitBtn.disabled = false;
    submitBtn.textContent = 'Process Settlement Run';
  }
}

function showBillingResult(summary) {
  const resultBox = document.getElementById('billing-run-result');
  document.getElementById('res-status').textContent = summary.status;
  document.getElementById('res-run-id').textContent = `Run ID: #${summary.runId}`;
  document.getElementById('res-vehicle-id').textContent = `#${summary.vehicleId}`;
  document.getElementById('res-month').textContent = summary.billingMonth;
  document.getElementById('res-item-count').textContent = `${summary.lineItemCount} trips`;
  document.getElementById('res-total-rupees').textContent = formatPaisa(summary.grandTotalPaisa);

  document.getElementById('res-btn-view-items').onclick = () => viewRunLineItems(summary.runId);
  document.getElementById('res-btn-download-pdf').onclick = () => downloadInvoicePdf(summary.runId);

  resultBox.classList.remove('hidden');
}

// --- Tab 3: Retrieve & Display Invoices ---

async function handleSearchRun() {
  const runIdInput = document.getElementById('input-search-run-id');
  const runId = runIdInput.value.trim();

  if (!runId) {
    showAlert('Please provide a valid Settlement Run ID.', 'error');
    return;
  }

  try {
    const res = await apiFetch(`/api/billing/run/${runId}`);
    const body = await res.json();

    if (!res.ok) {
      showAlert(body.message || `Settlement Run #${runId} not found.`, 'error');
      return;
    }

    const run = body.data;
    displayInvoiceDetail(run);
  } catch (err) {
    console.error('Error fetching run details:', err);
  }
}

function displayInvoiceDetail(run) {
  const card = document.getElementById('invoice-detail-card');
  document.getElementById('inv-header-title').textContent = `Settlement Statement #${run.id}`;
  document.getElementById('inv-header-meta').textContent = `Vehicle Asset: #${run.vehicleId} | Period: ${run.billingMonth}`;
  document.getElementById('inv-status').textContent = run.status;
  document.getElementById('inv-timestamp').textContent = run.runAt ? run.runAt.replace('T', ' ') : '-';
  document.getElementById('inv-trips-count').textContent = `${run.lineItemCount} trips`;
  document.getElementById('inv-grand-total').textContent = formatPaisa(run.grandTotalPaisa);

  document.getElementById('inv-btn-download-pdf').onclick = () => downloadInvoicePdf(run.id);

  const tbody = document.querySelector('#table-invoice-items tbody');
  const items = run.lineItems || [];

  if (items.length === 0) {
    tbody.innerHTML = '<tr><td colspan="7" class="text-center text-muted">No line items recorded for this run.</td></tr>';
  } else {
    tbody.innerHTML = items.map(item => `
      <tr>
        <td>#${item.id}</td>
        <td>Trip #${item.tripId}</td>
        <td>${formatPaisa(item.basePaisa)}</td>
        <td>${formatPaisa(item.extraChargesPaisa)}</td>
        <td>${formatPaisa(item.fixedFeeSharePaisa)}</td>
        <td><strong>${formatPaisa(item.totalPaisa)}</strong></td>
        <td><small class="text-muted">${item.computationNote || '-'}</small></td>
      </tr>
    `).join('');
  }

  card.classList.remove('hidden');
}

// --- Tab 5: Discrepancy & Fraud Audit ---

async function handleSearchAudit() {
  const runId = document.getElementById('input-audit-run-id').value.trim();
  if (!runId) {
    showAlert('Please enter a Settlement Run ID.', 'error');
    return;
  }

  try {
    const res = await apiFetch(`/api/billing/run/${runId}/flags`);
    const body = await res.json();

    if (!res.ok) {
      showAlert(body.message || 'Failed to retrieve audit flags.', 'error');
      return;
    }

    const flags = body.data || [];
    const tbody = document.querySelector('#table-audit-flags tbody');

    if (flags.length === 0) {
      tbody.innerHTML = '<tr><td colspan="6" class="text-center text-success">Clean audit: zero anomalies flagged for this run.</td></tr>';
    } else {
      tbody.innerHTML = flags.map(f => `
        <tr>
          <td>#${f.id}</td>
          <td>Run #${f.billingRunId}</td>
          <td>Trip #${f.tripId}</td>
          <td><span class="badge badge-warning">${f.flagType}</span></td>
          <td><span class="badge ${f.severity === 'HIGH' ? 'badge-danger' : 'badge-warning'}">${f.severity}</span></td>
          <td>${f.reason}</td>
        </tr>
      `).join('');
    }
  } catch (err) {
    console.error('Audit search error:', err);
  }
}

// --- PDF Download Service ---

async function downloadInvoicePdf(runId) {
  try {
    showAlert(`Compiling official invoice PDF for Run #${runId}...`, 'success');
    const response = await fetch(`/api/billing/run/${runId}/invoice/pdf`, {
      headers: {
        'Authorization': getAuthHeader()
      }
    });

    if (!response.ok) {
      throw new Error(`Failed to download PDF. Status: ${response.status}`);
    }

    const blob = await response.blob();
    const downloadUrl = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = downloadUrl;
    a.download = `fastfleet-invoice-run-${runId}.pdf`;
    document.body.appendChild(a);
    a.click();
    a.remove();
    window.URL.revokeObjectURL(downloadUrl);
  } catch (err) {
    console.error('PDF download error:', err);
    showAlert('Failed to download invoice PDF: ' + err.message, 'error');
  }
}

// --- Line Item Modal ---

async function viewRunLineItems(runId) {
  try {
    const res = await apiFetch(`/api/billing/run/${runId}`);
    const body = await res.json();
    if (!res.ok) throw new Error(body.message || 'Run details not found');

    const run = body.data;
    document.getElementById('modal-title').textContent = `Settlement Breakdown — Run #${run.id} (${run.billingMonth})`;
    document.getElementById('modal-summary').textContent = `Vehicle #${run.vehicleId} | Total: ${formatPaisa(run.grandTotalPaisa)} across ${run.lineItemCount} trips`;

    document.getElementById('modal-btn-download-pdf').onclick = () => downloadInvoicePdf(run.id);

    const tbody = document.getElementById('modal-items-tbody');
    const items = run.lineItems || [];

    if (items.length === 0) {
      tbody.innerHTML = '<tr><td colspan="6" class="text-center text-muted">No line items</td></tr>';
    } else {
      tbody.innerHTML = items.map(item => `
        <tr>
          <td>Trip #${item.tripId}</td>
          <td>${formatPaisa(item.basePaisa)}</td>
          <td>${formatPaisa(item.extraChargesPaisa)}</td>
          <td>${formatPaisa(item.fixedFeeSharePaisa)}</td>
          <td><strong>${formatPaisa(item.totalPaisa)}</strong></td>
          <td><small class="text-muted">${item.computationNote || '-'}</small></td>
        </tr>
      `).join('');
    }

    itemsModal.classList.remove('hidden');
  } catch (err) {
    showAlert('Failed to inspect line items: ' + err.message, 'error');
  }
}

function closeModal() {
  if (itemsModal) itemsModal.classList.add('hidden');
}

// --- Event Listeners & Startup ---

document.addEventListener('DOMContentLoaded', () => {
  initTabs();

  // Button clicks
  const refreshOverviewBtn = document.getElementById('btn-refresh-overview');
  if (refreshOverviewBtn) refreshOverviewBtn.addEventListener('click', loadTelemetry);

  const refreshFleetBtn = document.getElementById('btn-refresh-fleet');
  if (refreshFleetBtn) refreshFleetBtn.addEventListener('click', loadVehicles);

  const billingForm = document.getElementById('billing-run-form');
  if (billingForm) billingForm.addEventListener('submit', handleBillingSubmit);

  const searchRunBtn = document.getElementById('btn-search-run');
  if (searchRunBtn) searchRunBtn.addEventListener('click', handleSearchRun);

  const searchAuditBtn = document.getElementById('btn-search-audit');
  if (searchAuditBtn) searchAuditBtn.addEventListener('click', handleSearchAudit);

  // Close modal when clicking on backdrop
  if (itemsModal) {
    itemsModal.addEventListener('click', (e) => {
      if (e.target === itemsModal) closeModal();
    });
  }

  // Initial data loading
  loadTelemetry();
  loadVehicles();
});
