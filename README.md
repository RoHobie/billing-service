# Rental Fleet Billing Service

A comprehensive commercial fleet billing microservice built with Spring Boot 3, Redis caching, and Actuator telemetry. It computes deterministic, paisa-accurate month-end invoices for rental fleet vehicles across tiered-slab, flat-rate, and fixed-monthly contracts. Features include exact fee distribution via the Largest Remainder Method, application-level idempotency, mid-month contract versioning, advisory fraud detection, downloadable corporate PDF invoices, and resilient Redis caching.

---

## Prerequisites & Dependencies

### When Running With Docker
- **Docker Engine**: version 24.0+
- **Docker Compose**: version 2.20+
- *No local Java, Maven, PostgreSQL, or Redis installations are required on the host system.*

### When Running Locally (Without Docker)
- **Java Development Kit (JDK)**: OpenJDK or Eclipse Temurin **21 (LTS)**
- **Build Tool**: Maven 3.9+ (optional — the included `./mvnw` wrapper can be used directly)
- **Storage Engines**:
  - **H2 (Default)**: Embedded in-memory database managed inside the JVM process. **Zero installation required.**
  - **PostgreSQL 16+ (Persistent Storage)**:
    - *Ubuntu / Debian*: `sudo apt update && sudo apt install -y postgresql postgresql-contrib`
    - *macOS (Homebrew)*: `brew install postgresql@16 && brew services start postgresql@16`
    - *Or spin up just the containerized DB*: `docker compose up -d postgres`
    - *Default credentials*: Database `fleetdb`, User `postgres`, Password `postgres`, Port `5432`.
- **Cache Engine (Optional)**:
  - Redis 7+ (`sudo apt install redis-server` or `docker compose up -d redis`). If omitted, the service falls back automatically to direct database reads.

---

## Quickstart & Storage Configuration

The system supports **H2 in-memory storage (default)** for zero-setup execution and unit testing, and **PostgreSQL (persistent storage)** backed by Docker volumes for production-like persistent fleet operations.

### 1. Running with Docker Compose

The `docker-compose.yml` orchestrates the Billing microservice, Redis cache, and PostgreSQL with a dedicated persistent named volume (`postgres-data`).

#### Mode A: Default In-Memory Storage (H2)
```bash
docker compose up --build
```
- **H2 Console**: `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:fleetdb`, User: `sa`, Password: empty)
- **Actuator Health**: `http://localhost:8080/actuator/health`
- **Redis Cache**: `localhost:6379`

#### Mode B: Persistent Storage (PostgreSQL)
To run with PostgreSQL and persist all data across container restarts in the `postgres-data` volume:
```bash
SPRING_PROFILES_ACTIVE=postgres docker compose up --build
```
- **PostgreSQL Port**: `localhost:5432` (`fleetdb` database)
- **Data Persistence**: Stored in Docker volume `fastfleet_postgres-data`

---

### 2. Running Locally (Without Docker)

#### Mode A: Default In-Memory Storage (H2)
```bash
# 1. Navigate to billing module
cd billing

# 2. Run unit and integration tests (50 passing tests)
./mvnw test

# 3. Start the application with default H2 in-memory database
./mvnw spring-boot:run
```

#### Mode B: Persistent Storage (PostgreSQL)
Ensure PostgreSQL is running locally on port 5432 with database `fleetdb` (or run `docker compose up -d postgres redis`), then start the service with the `postgres` profile:
```bash
cd billing

# Via Maven profile flag:
./mvnw spring-boot:run -Dspring-boot.run.profiles=postgres

# Or via environment variable:
SPRING_PROFILES_ACTIVE=postgres ./mvnw spring-boot:run
```

### Storage Configuration Matrix

| Environment Variable | Default Value | Description |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `default` | Active Spring profile (`default` for H2 in-memory, `postgres` for PostgreSQL) |
| `POSTGRES_HOST` | `localhost` (`postgres` in Docker) | PostgreSQL hostname |
| `POSTGRES_PORT` | `5432` | PostgreSQL port |
| `POSTGRES_DB` | `fleetdb` | Target database name |
| `POSTGRES_USER` | `postgres` | Database username |
| `POSTGRES_PASSWORD` | `postgres` | Database password |
| `REDIS_HOST` | `localhost` (`redis` in Docker) | Redis hostname |
| `REDIS_PORT` | `6379` | Redis port |

The application automatically seeds demonstration fleet data on startup via `DataLoader` (vendors, vehicles, tiered-slab contracts, mid-month revisions, and January 2026 duty trips). In PostgreSQL mode, existing records are detected on subsequent boots to prevent duplicate seeding.

---

## PDF Invoicing & In-Memory Generation

Official corporate PDF invoices are compiled on the fly using LibrePDF/OpenPDF:

- **Endpoint**: `GET /api/billing/run/{runId}/invoice/pdf`
- **Content-Type**: `application/pdf` (attachment header `invoice-{runId}.pdf`)
- **Structure**:
  - FastFleet Corporate Header and GSTIN compliance credentials
  - Invoice metadata and vehicle asset registry block
  - Financial breakdown card (Base Fare, Surcharges, Fixed Contract Share, Grand Total in ₹)
  - Itemised per-trip audit table with distances, surcharges, and calculation trace notes
  - Net-30 payment terms and accounts sign-off footer

---

## Redis Caching & Eviction Policies

Redis provides high-speed sub-millisecond retrieval of hot operational data:

| Cache Name | Key Pattern | TTL | Eviction Policy |
|---|---|---|---|
| `contracts` | `{vehicleId}:{tripDate}` | 15 mins | Evicted on new contract registration or slab addition (`@CacheEvict(allEntries = true)`) |
| `vehicles` | `{id}` | 15 mins | Evicted on new vehicle registration (`@CacheEvict(allEntries = true)`) |
| `billingSummaries` | `{runId}` | 15 mins | Evicted on new billing run execution (`@CacheEvict(allEntries = true)`) |

**Graceful Degradation**: Configured with a custom `CacheErrorHandler` (`RedisConfig`). If Redis is unreachable or temporarily offline, all cache read/write operations log a warning and fallback seamlessly to database queries without throwing 500 errors to callers.

---

## System Monitoring & Actuator Telemetry

1. **Custom Business Telemetry Endpoint**:
   - `GET /api/monitoring/stats` (Requires `ADMIN` or `FINANCE` role)
   - Returns real-time active vehicles, vendor count, billed revenue in paisa, completed runs, flagged anomalies, JVM memory usage (used/max MB), and system uptime seconds.
2. **Micrometer Counters & Timers**:
   - `fleet.billing.runs.total`: Total completed billing runs.
   - `fleet.billing.revenue.paisa`: Cumulative revenue processed in paisa.
   - `fleet.billing.fraud.flags`: Total advisory fraud anomalies detected.
   - `fleet.billing.run.duration`: High-resolution execution duration timer for billing calculations.
3. **Spring Boot Actuator**:
   - `GET /actuator/health` (Public liveness and readiness check)
   - `GET /actuator/metrics` (`ADMIN` access)

---

## Authentication & Roles

The service is secured with HTTP Basic Authentication and Role-Based Access Control (RBAC):

| User | Password | Role | Permissions |
|---|---|---|---|
| `admin` | `admin123` | `ADMIN` | Trigger billing runs, create master data (vendors, vehicles, contracts, slabs, trips), view bills, view metrics |
| `finance` | `finance123` | `FINANCE` | Read-only access to view bills, itemised line items, summaries, fraud flags, and telemetry stats |

---

## Sample cURL Commands

### 1. Download Official PDF Invoice (`ADMIN` or `FINANCE`)

```bash
curl -i -X GET http://localhost:8080/api/billing/run/1/invoice/pdf \
  -u finance:finance123 \
  -o invoice-1.pdf
```

### 2. Retrieve Real-Time System Telemetry (`ADMIN` or `FINANCE`)

```bash
curl -i -X GET http://localhost:8080/api/monitoring/stats \
  -u finance:finance123
```

### 3. Trigger Month-End Billing Run (`ADMIN` only)

```bash
curl -i -X POST http://localhost:8080/api/billing/run \
  -u admin:admin123 \
  -H "Content-Type: application/json" \
  -d '{"vehicleId": 2, "billingMonth": "2026-01"}'
```

### 4. View Full Itemised Bill with Line Items (`FINANCE` or `ADMIN`)

```bash
curl -i -X GET http://localhost:8080/api/billing/run/1 \
  -u finance:finance123
```

### 5. Inspect Advisory Fraud Flags (`FINANCE` or `ADMIN`)

```bash
curl -i -X GET http://localhost:8080/api/billing/run/1/flags \
  -u finance:finance123
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

### 5. Multi-Layered Idempotency & Concurrency Safety
Executing `POST /api/billing/run` for `(vehicleId, billingMonth)` is protected at both application and database layers:
1. **Application Query Check**: First checks for an existing `COMPLETED` run. If found, returns the persisted summary immediately with zero duplicate computation.
2. **Database Unique Constraint**: A database constraint on `(vehicle_id, billing_month)` prevents concurrent execution threads from creating duplicate runs. If a race condition occurs, `DataIntegrityViolationException` is caught, the existing run is re-queried, and its summary is returned with HTTP 200 OK.
3. **Optimistic Locking**: An `@Version` field on `BillingRun` ensures database-level concurrency protection, eliminating lost updates.

---

## Handling System Failure Cases & Recovery Procedures

Billing is a **pure function** of immutable historical `Trip` records and `Contract` rules. A `FAILED` or interrupted run is safely recoverable simply by re-running; nothing is lost because nothing but derived output was ever mutated. 

When a re-run is requested for an incomplete, stale, or failed run:
1. The orchestrator resets the run status to `PENDING` within an atomic transaction.
2. Stale partial line items and fraud flags from the aborted run are deleted.
3. The calculation re-evaluates all trip fares from the ground truth trip and contract tables.
4. Line items and advisory fraud flags are re-persisted and the status transitions to `COMPLETED`.

Because inputs are never mutated, disaster recovery requires zero manual database rollback scripts or data restoration procedures.

---

## Paginated Line Items API

For fleets logging thousands of trips per month, line items can be retrieved in paginated slices:

- **Endpoint**: `GET /api/billing/run/{runId}/items?page=0&size=20`
- **Roles**: `ADMIN`, `FINANCE`
- **Query Parameters**:
  - `page`: 0-indexed page number (default `0`).
  - `size`: number of records per page (default `20`).
  - `sort`: optional sort property (e.g. `trip.startTime,asc`).
- **Response**: Standard Spring Data Page structure containing `content` array of `BillLineItemResponse`, `totalPages`, `totalElements`, `size`, and `number`.

Full non-paginated bill data remains available at `GET /api/billing/run/{runId}` for compliance archiving and PDF generation.

---

## Algorithmic Complexity & Cost Estimation

A formal time and space complexity evaluation for every computation point utilizing auxiliary memory is documented in [COMPLEXITY_ANALYSIS.md](COMPLEXITY_ANALYSIS.md), covering:
- $O(N \log N)$ time and $O(N)$ auxiliary memory for the Largest Remainder Method (`FixedFeeSplitService.split`).
- $O(N \log N)$ time and $O(N)$ space for Month-End Orchestration (`BillingRunService.runBilling`).
- $O(N)$ linear-time advisory fraud detection (`FraudDetectionService.scan`).
- $O(P)$ constant memory footprint for paginated query slices.
- $O(V \times D)$ bounded in-memory cache footprint in Redis.

---

## Assumptions & Design Decisions

1. **Cumulative Tiered Slabs**: Slab rates apply cumulatively to total trip distance, not flat over the entire distance.
2. **Dead-Leg Trips in Distribution**: Dead-leg trips receive 0 base fare but receive their proportional share of fixed monthly fees because the vehicle was occupied on company business.
3. **Free Kilometers**: Contract free km reduce the billable trip distance before walking slabs.
4. **Integer Distance**: Trip distance is measured in integer kilometers.
5. **Pass-Through Tolls**: Toll fees are added at direct cost with no markup.
6. **Impossible Distance Threshold**: Single trips with distance exceeding 500 km trigger an advisory `IMPOSSIBLE_DISTANCE` fraud flag.
7. **Billing Month Format**: Month inputs must adhere to `YYYY-MM`.
8. **Long Paisa vs BigDecimal for Storage**: `long` is faster, memory-efficient, and guarantees integer precision. `BigDecimal` is restricted to division operations.
9. **Pure Function Billing**: Billing never mutates raw trip or contract data, guaranteeing fault-tolerant recovery.
