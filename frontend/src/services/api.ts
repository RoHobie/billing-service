import {
  User,
  Vehicle,
  Vendor,
  BillingRunSummary,
  BillLineItem,
  FraudFlag,
  SystemStats,
  Page,
  TripRecord
} from '../types';

let currentAuthHeader: string | null = sessionStorage.getItem('billing_auth_header');

export function setAuthCredentials(username: string, password: string): string {
  const token = btoa(`${username}:${password}`);
  currentAuthHeader = `Basic ${token}`;
  sessionStorage.setItem('billing_auth_header', currentAuthHeader);
  return currentAuthHeader;
}

export function clearAuthCredentials(): void {
  currentAuthHeader = null;
  sessionStorage.removeItem('billing_auth_header');
  sessionStorage.removeItem('billing_user');
}

export function getStoredUser(): User | null {
  const saved = sessionStorage.getItem('billing_user');
  if (!saved) return null;
  try {
    return JSON.parse(saved);
  } catch {
    return null;
  }
}

export function setStoredUser(user: User): void {
  sessionStorage.setItem('billing_user', JSON.stringify(user));
}

async function apiFetch<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
  const headers = new Headers(options.headers || {});
  if (currentAuthHeader) {
    headers.set('Authorization', currentAuthHeader);
  }
  if (!headers.has('Content-Type') && options.body && typeof options.body === 'string') {
    headers.set('Content-Type', 'application/json');
  }

  const response = await fetch(endpoint, {
    ...options,
    headers,
  });

  if (!response.ok) {
    let errorMessage = `Request failed (${response.status})`;
    try {
      const errorJson = await response.json();
      if (errorJson.message) {
        errorMessage = errorJson.message;
      } else if (errorJson.error) {
        errorMessage = errorJson.error;
      }
    } catch {
      // response was not JSON
    }
    throw new Error(errorMessage);
  }

  const json = await response.json();
  return json.data !== undefined ? json.data : json;
}

export const api = {
  async authenticate(username: string, password: string): Promise<User> {
    const authHeader = setAuthCredentials(username, password);
    const response = await fetch('/api/auth/me', {
      headers: {
        'Authorization': authHeader,
      },
    });

    if (!response.ok) {
      clearAuthCredentials();
      throw new Error(response.status === 401 ? 'Invalid username or password' : 'Authentication failed');
    }

    const json = await response.json();
    const user: User = {
      username: json.data.username,
      role: json.data.role,
    };
    setStoredUser(user);
    return user;
  },

  async registerVendor(data: {
    username: string;
    password: string;
    vendorName?: string;
    contactEmail?: string;
  }): Promise<User> {
    await apiFetch('/api/auth/register', {
      method: 'POST',
      body: JSON.stringify(data),
    });

    // Auto login with registered credentials
    return this.authenticate(data.username, data.password);
  },

  async getSystemStats(): Promise<SystemStats> {
    return apiFetch<SystemStats>('/api/monitoring/stats');
  },

  async getVehicles(vendorId?: number): Promise<Vehicle[]> {
    const url = vendorId ? `/api/vehicles?vendorId=${vendorId}` : '/api/vehicles';
    return apiFetch<Vehicle[]>(url);
  },

  async getVehiclesByVendor(vendorId: number): Promise<Vehicle[]> {
    return apiFetch<Vehicle[]>(`/api/vendors/${vendorId}/vehicles`);
  },

  async getVendors(): Promise<Vendor[]> {
    return apiFetch<Vendor[]>('/api/vendors');
  },

  async getBillingRuns(vehicleId?: number): Promise<BillingRunSummary[]> {
    const url = vehicleId ? `/api/billing/runs?vehicleId=${vehicleId}` : '/api/billing/runs';
    return apiFetch<BillingRunSummary[]>(url);
  },

  async getTripsByVehicle(vehicleId: number): Promise<TripRecord[]> {
    return apiFetch<TripRecord[]>(`/api/trips?vehicleId=${vehicleId}`);
  },

  async runBilling(vehicleId: number, billingMonth: string): Promise<BillingRunSummary> {
    return apiFetch<BillingRunSummary>('/api/billing/run', {
      method: 'POST',
      body: JSON.stringify({ vehicleId, billingMonth }),
    });
  },

  async getBillingRunSummary(runId: number): Promise<BillingRunSummary> {
    return apiFetch<BillingRunSummary>(`/api/billing/run/${runId}/summary`);
  },

  async getBillingRunFlags(runId: number): Promise<FraudFlag[]> {
    return apiFetch<FraudFlag[]>(`/api/billing/run/${runId}/flags`);
  },

  async getBillingRunItems(runId: number, page: number = 0, size: number = 20): Promise<Page<BillLineItem>> {
    return apiFetch<Page<BillLineItem>>(`/api/billing/run/${runId}/items?page=${page}&size=${size}`);
  },

  async downloadInvoicePdf(runId: number): Promise<void> {
    const headers = new Headers();
    if (currentAuthHeader) {
      headers.set('Authorization', currentAuthHeader);
    }
    const response = await fetch(`/api/billing/run/${runId}/invoice/pdf`, { headers });
    if (!response.ok) {
      throw new Error('Failed to download invoice PDF');
    }
    const blob = await response.blob();
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `Invoice-Statement-Run-${runId}.pdf`;
    document.body.appendChild(a);
    a.click();
    a.remove();
    window.URL.revokeObjectURL(url);
  },
};
