export interface User {
  username: string;
  role: 'ADMIN' | 'FINANCE';
}

export interface Vehicle {
  id: number;
  registrationNumber: string;
  vehicleType?: string;
  type?: string;
  vendorId?: number;
  vendorName: string;
}

export interface Vendor {
  id: number;
  name: string;
  contactEmail: string;
}

export interface BillingRunSummary {
  runId: number;
  vehicleId: number;
  billingMonth: string;
  status: string;
  lineItemCount: number;
  grandTotalPaisa: number;
}

export interface BillLineItem {
  id: number;
  tripId: number;
  basePaisa: number;
  extraChargesPaisa: number;
  fixedFeeSharePaisa: number;
  totalPaisa: number;
  computationNote: string;
  distanceKm?: number;
  startTime?: string;
}

export interface TripRecord {
  id: number;
  vehicleId: number;
  vehicleRegistrationNumber: string;
  startTime: string;
  endTime: string;
  distanceKm: number;
  isDeadLeg: boolean;
  hasNightCharge: boolean;
  waitingMinutes: number;
  tollAmountPaisa: number;
}

export interface FraudFlag {
  id: number;
  tripId: number;
  flagType: string;
  description: string;
}

export interface SystemStats {
  totalVehicles: number;
  totalVendors: number;
  totalBillingRuns: number;
  totalRevenuePaisa: number;
  totalFraudFlags: number;
  uptimeSeconds: number;
  systemHealth: string;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export function formatCurrency(paisa: number): string {
  const rupees = (paisa || 0) / 100;
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(rupees);
}
