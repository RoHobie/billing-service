# Rental Fleet Billing Service

A backend billing microservice built with Spring Boot 3 that computes deterministic, paisa-accurate month-end invoices for rental fleet vehicles across tiered-slab, flat-rate, and fixed-monthly contracts. It features exact fee distribution via the Largest Remainder Method, application-level idempotency, mid-month contract versioning, and advisory fraud detection.

---

## Quickstart (< 5 Commands)

```bash
# 1. Navigate to billing module
cd billing

# 2. Run unit and integration tests
./mvnw test

# 3. Start the application
./mvnw spring-boot:run
```

The application automatically seeds demonstration data on startup via `DataLoader`, including vendors, vehicles, tiered-slab contracts, mid-month revisions, and January 2026 trips.

---

## H2 Console Access

An in-memory H2 database is enabled for rapid development and audit inspection:

- **URL**: `http://localhost:8080/h2-console`
- **JDBC URL**: `jdbc:h2:mem:fleetdb`
- **Username**: `sa`
- **Password**: *(leave blank)*

---

## Authentication & Roles

The service is secured with HTTP Basic Authentication and Role-Based Access Control (RBAC):

| User | Password | Role | Permissions |
|---|---|---|---|
| `admin` | `admin123` | `ADMIN` | Trigger billing runs, create master data (vendors, vehicles, contracts, slabs, trips), view bills |
| `finance` | `finance123` | `FINANCE` | Read-only access to view bills, itemised line items, summaries, and fraud flags |

---

## Sample cURL Commands

### 1. Trigger Month-End Billing Run (`ADMIN` only)

```bash
curl -i -X POST http://localhost:8080/api/billing/run \
  -u admin:admin123 \
  -H "Content-Type: application/json" \
  -d '{"vehicleId": 2, "billingMonth": "2026-01"}'
```

### 2. View Full Itemised Bill with Line Items (`FINANCE` or `ADMIN`)

```bash
curl -i -X GET http://localhost:8080/api/billing/run/1 \
  -u finance:finance123
```

### 3. View Grand Total & Summary (`FINANCE` or `ADMIN`)

```bash
curl -i -X GET http://localhost:8080/api/billing/run/1/summary \
  -u finance:finance123
```

### 4. Inspect Advisory Fraud Flags (`FINANCE` or `ADMIN`)

```bash
curl -i -X GET http://localhost:8080/api/billing/run/1/flags \
  -u finance:finance123
```

### 5. Master Data Setup (Create Vendor, Vehicle, Contract, Slabs, Trip)

```bash
# Create Vendor
curl -i -X POST http://localhost:8080/api/vendors \
  -u admin:admin123 \
  -H "Content-Type: application/json" \
  -d '{"name": "Metro Cabs Ltd", "contactEmail": "billing@metrocabs.com"}'

# Create Vehicle
curl -i -X POST http://localhost:8080/api/vehicles \
  -u admin:admin123 \
  -H "Content-Type: application/json" \
  -d '{"registrationNumber": "DL-01-XY-9000", "type": "Sedan", "vendorId": 1}'

# Create Tiered-Slab Contract (Effective Jan 1, 2026)
curl -i -X POST http://localhost:8080/api/contracts \
  -u admin:admin123 \
  -H "Content-Type: application/json" \
  -d '{"vehicleId": 1, "vendorId": 1, "contractType": "PER_KM", "baseAmountPaisa": 0, "freeKm": 0, "effectiveFrom": "2026-01-01", "nightChargePaisa": 500, "waitingRatePerMinutePaisa": 200}'

# Add Tier 1 Slab (0-100 km @ ₹12 = 1200 paisa)
curl -i -X POST http://localhost:8080/api/contracts/1/slabs \
  -u admin:admin123 \
  -H "Content-Type: application/json" \
  -d '{"fromKm": 0, "toKm": 100, "ratePerKmPaisa": 1200}'

# Add Tier 2 Slab (101-300 km @ ₹10 = 1000 paisa)
curl -i -X POST http://localhost:8080/api/contracts/1/slabs \
  -u admin:admin123 \
  -H "Content-Type: application/json" \
  -d '{"fromKm": 101, "toKm": 300, "ratePerKmPaisa": 1000}'

# Record a Duty Trip
curl -i -X POST http://localhost:8080/api/trips \
  -u admin:admin123 \
  -H "Content-Type: application/json" \
  -d '{"vehicleId": 1, "startTime": "2026-01-10T09:00:00", "endTime": "2026-01-10T12:00:00", "distanceKm": 150, "isDeadLeg": false, "hasNightCharge": false, "waitingMinutes": 10, "tollAmountPaisa": 15000}'
```

---

## Core Algorithms & Precision Engineering

### 1. Money Is Always Paisa (`long`)
To eliminate IEEE 754 floating-point inaccuracies (`0.1 + 0.2 != 0.3`), all monetary amounts across entities, DTOs, and services are stored and transferred strictly as integer `long` paisa (1 Rupee = 100 paisa). Intermediate divisions use `BigDecimal` at scale 10 with `RoundingMode.HALF_UP` and are immediately converted back to `long`. No `double` or `float` types exist in monetary paths.

### 2. Tiered Slab Walk (Tax-Slab Style)
Distance is walked progressively across contiguous slabs (0–100, 101–200, etc.). For instance, a 150 km trip under `[0–100 @ ₹50, 101–200 @ ₹45]` costs:
$$\text{Cost} = (100 \times 5000\text{p}) + (50 \times 4500\text{p}) = 500,000\text{p} + 225,000\text{p} = 725,000\text{p} \quad (\text{₹}7,250)$$

### 3. Exact Fixed Monthly Fee Split (Largest Remainder Method)
When distributing a fixed monthly fee (e.g. ₹30,000 across 5,000 trips), integer division causes truncation error. The Largest Remainder Method guarantees exact sum reconciliation:
1. Every trip receives $\lfloor \text{exactShare} \rfloor$.
2. The remaining deficit $R = \text{ContractTotal} - \sum \text{floorShares}$ is distributed $1\text{ paisa}$ each to the trips having the highest fractional remainders.
3. Sum of all trip shares is mathematically asserted to equal the contract fee exactly down to the last paisa.

### 4. Mid-Month Contract Versioning
Contracts carry an `effectiveFrom` date. For each trip, the active contract is resolved via:
```sql
SELECT c FROM Contract c WHERE c.vehicle.id = :vehicleId AND c.effectiveFrom <= :tripDate ORDER BY c.effectiveFrom DESC
```
This cleanly applies rate adjustments mid-month (e.g. trips on Jan 14 apply Jan 1 rates, while trips on Jan 16 apply Jan 15 rates).

### 5. Application-Level Idempotency
Executing `POST /api/billing/run` for `(vehicleId, billingMonth)` first checks for an existing `COMPLETED` run. If found, it immediately returns the saved result without redundant computation or duplicate database records.

---

## Assumptions

1. **Cumulative Tiered Slabs**: Slab rates apply cumulatively to total trip distance (like income tax brackets), not as a single flat rate for the entire distance.
2. **Dead-Leg Trips in Distribution**: Dead-leg (empty repositioning) runs are included in the fixed-fee split denominator because the vehicle was operating on company duty. Dead-leg trips receive 0 base fare but receive their proportional share of fixed monthly fees.
3. **Free Kilometers**: Contract free km reduce the billable trip distance before walking slabs.
4. **Integer Distance**: Trip distance is measured in integer kilometers.
5. **Pass-Through Tolls**: Toll fees are added at direct cost with no markup.
6. **Impossible Distance Threshold**: Single trips with distance exceeding 500 km trigger an advisory `IMPOSSIBLE_DISTANCE` fraud flag.
7. **Billing Month Format**: Month inputs must adhere to `YYYY-MM`.

---

## Design Decisions & Trade-Offs

- **Long Paisa vs BigDecimal for Storage**: `long` is faster, memory-efficient, and guarantees integer precision. `BigDecimal` is restricted to division operations.
- **`effectiveFrom` on Contract vs Separate Versioning Join Table**: Using `effectiveFrom` avoids joins, date range overlaps, and redundant version management endpoints while naturally solving mid-month rate changes.
- **Advisory vs Blocking Fraud Flags**: Fraud flags alert finance teams without blocking bill generation, preventing business bottlenecks in the absence of a manual approval workflow.
- **Application-Level Idempotency vs DB Unique Constraint**: Enables clean handling of retry logic for failed runs while remaining fully testable in memory. For distributed production, optimistic locking and DB constraints can be layered on.

---

## Known Limitations

- **Completed Run Invalidation**: Once marked `COMPLETED`, a run cannot be modified via API if a contract is retroactively altered (documenting for future invalidation endpoint).
- **In-Memory H2 Persistence**: Database state resets on application shutdown.
- **Pagination**: Line items are returned as a full list (acceptable for MVP scale; production would add Spring Data `Pageable`).

---

## Swapping to PostgreSQL for Production

To transition from H2 to PostgreSQL, simply update `pom.xml` and `application.properties`:

1. Replace `com.h2database:h2` with `org.postgresql:postgresql` in `pom.xml`.
2. Update `application.properties`:
   ```properties
   spring.datasource.url=jdbc:postgresql://localhost:5432/fleetdb
   spring.datasource.username=postgres
   spring.datasource.password=postgres
   spring.datasource.driver-class-name=org.postgresql.Driver
   spring.jpa.hibernate.ddl-auto=validate
   ```
No Java code modifications are necessary due to Spring Data JPA abstraction.
