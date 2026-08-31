# DotLabs - Money Transfer Service

[![Java](https://img.shields.io/badge/Java-17%2B-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.3-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Build Status](https://img.shields.io/badge/Build-Passing-success.svg)]()
[![Kubernetes](https://img.shields.io/badge/Kubernetes-Ready-blue.svg)](https://kubernetes.io/)

A production-grade Java Spring Boot microservice designed to simulate secure money transfers between bank accounts with automated transaction fee calculation, scheduled commission evaluation, daily aggregation reporting, and distributed concurrency protection tailored for Kubernetes multi-instance environments.

---

## 📋 Table of Contents
1. [Key Features](#-key-features)
2. [Architecture & Design Patterns](#-architecture--design-patterns)
3. [Business Rules & Formulas](#-business-rules--formulas)
4. [Kubernetes Multi-Pod & Concurrency Strategy](#-kubernetes-multi-pod--concurrency-strategy)
5. [Getting Started & Running Locally](#-getting-started--running-locally)
6. [API Documentation & cURL Examples](#-api-documentation--curl-examples)
7. [Running with Docker & Kubernetes](#-running-with-docker--kubernetes)
8. [Automated Testing & Concurrency Verification](#-automated-testing--concurrency-verification)
9. [Pre-seeded Test Accounts](#-pre-seeded-test-accounts)

---

## 🚀 Key Features

* **Money Transfer Processing (`POST /api/v1/transfers`)**:
  - Simulates money transfer between bank accounts with real-time balance updates.
  - Automatic transaction fee calculation (**0.5% capped at 100.00**).
  - Total billed amount computed as `amount + transactionFee`.
  - Atomicity, balance checks, and status tracking (`SUCCESSFUL`, `INSUFFICIENT FUND`, `FAILED`, `ACCOUNT_NOT_FOUND`).

* **Dynamic Transaction Querying (`GET /api/v1/transactions`)**:
  - Filter transactions by `status`, `accountNumber` (sender or recipient), and date range (`startDate`, `endDate`).
  - Full pagination and sorting support.

* **Scheduled Commission Analysis Job (`POST /api/v1/commissions/run-analysis` & Cron)**:
  - Daily scheduled operation (default: `01:00 AM`) analyzing successful transactions.
  - Updates transactions as `commissionWorthy = true` with commission calculated at **20% of the transaction fee** (capped at 20.00).
  - Multi-instance distributed locking via **ShedLock**.

* **Daily Transaction Summary (`GET /api/v1/summaries/daily` & Cron)**:
  - Generates aggregate metrics (total transactions, volume, fees, commission, status breakdown) for any specified date (present and past).
  - Daily scheduled archiving job (default: `02:00 AM`) with ShedLock.

* **Production Readiness**:
  - **SpringDoc OpenAPI / Swagger UI** for interactive exploration.
  - **Spring Boot Actuator** health probes (`/actuator/health/liveness`, `/actuator/health/readiness`, `/actuator/metrics`, `/actuator/prometheus`).
  - Multi-stage **Dockerfile**, **docker-compose.yml**, and **Kubernetes manifests** (`k8s/`).

---

## 🏛️ Architecture & Design Patterns

The service adheres strictly to clean architecture and enterprise patterns:
* **Layered Architecture**: Clear separation of concerns:
  - `controller`: REST APIs, OpenAPI docs, input validation (`@Valid`).
  - `service`: Service interfaces and `service.impl` package implementations for business logic.
  - `repository`: Spring Data JPA with `JpaSpecificationExecutor` for dynamic queries and `@Lock(LockModeType.PESSIMISTIC_WRITE)` for concurrency isolation.
  - `entity` / `dto`: Encapsulated domain entities and immutable DTO records.
  - `scheduler`: ShedLock-guarded cron workers.
  - `exception`: Centralized `@RestControllerAdvice` exception handler.
* **Database Versioning (Flyway)**:
  - `V1__init_schema.sql`: Automated schema creation for accounts, transactions, daily summaries, and ShedLock.
  - `V2__seed_initial_accounts.sql`: Pre-loaded test accounts and balances.
* **Design Patterns Employed**:
  - **Strategy & Utility Pattern**: `FeeCalculator` encapsulating financial rounding and capping rules.
  - **Specification Pattern**: `TransactionSpecification` constructing dynamic JPA Criteria predicates.
  - **Builder Pattern**: Fluent entity and DTO instantiation.
  - **Distributed Lock Pattern**: ShedLock JDBC Provider ensuring single-leader job execution across Kubernetes pods.

---

## 📐 Business Rules & Formulas

### 1. Transaction Fee
$$\text{Fee} = \min(\text{Amount} \times 0.005, 100.00)$$
* Fee is **0.5%** of the transfer principal amount.
* Maximum fee cap is **100.00**.
* Total amount debited from sender: $\text{Billed Amount} = \text{Amount} + \text{Fee}$.

### 2. Commission
$$\text{Commission} = \text{Transaction Fee} \times 0.20$$
* Commission is **20%** of the collected transaction fee.
* Maximum commission per transaction is **20.00** ($100.00 \times 20\%$).
* Non-successful transactions (e.g. `INSUFFICIENT FUND`) have `commissionWorthy = false` and `commission = 0.00`.

---

## 🛡️ Kubernetes Multi-Pod & Concurrency Strategy

The assessment explicitly notes: *"We run multiple instances of our services in a kubernetes cluster."*

To ensure full resilience in a multi-pod cluster:

1. **Deadlock Prevention in Balance Transfers**:
   - When transferring between Account A and Account B, accounts are locked in a deterministic order based on account numbers (`source.compareTo(dest) < 0 ? lock source then dest : lock dest then source`).
   - This eliminates circular wait deadlocks when Account A transfers to Account B at the exact same moment Account B transfers to Account A.

2. **Distributed Scheduled Tasks (ShedLock)**:
   - When 5 replicas of the service run in Kubernetes, both the Commission Analysis job and Daily Summary job use `@SchedulerLock` backed by the SQL `shedlock` table.
   - Only a single pod acquires the execution lock; other pods safely skip execution.

3. **Kubernetes Health & Autoscaling**:
   - `livenessProbe` and `readinessProbe` wired to Spring Boot Actuator endpoints.
   - `k8s/hpa.yaml` configured to autoscale pods between 2 and 10 replicas based on CPU/memory utilization.

---

## 💻 Getting Started & Running Locally

### Prerequisites
* Java 17+ installed (`java -version`)
* Maven is **not required** (the project includes the Maven wrapper `./mvnw` / `mvnw.cmd`)

### Clone & Run
```bash
# Navigate to the project directory
cd dotlabs-money-transfer

# Run the application with Maven wrapper
./mvnw spring-boot:run        # On Linux / macOS
.\mvnw.cmd spring-boot:run    # On Windows
```

The application starts on `http://localhost:8080`.

### Interactive Swagger UI
Open your browser and navigate to:
👉 **`http://localhost:8080/swagger-ui.html`**

---

## 📡 API Documentation & cURL Examples

### 1. Bank Accounts (Demo & Balance Check)
```bash
# Get all accounts with current balances
curl -X GET http://localhost:8080/api/v1/accounts

# Get specific account balance
curl -X GET http://localhost:8080/api/v1/accounts/1000000001
```

### 2. Money Transfer (`POST /api/v1/transfers`)
```bash
curl -X POST http://localhost:8080/api/v1/transfers \
  -H "Content-Type: application/json" \
  -d '{
    "sourceAccountNumber": "1000000001",
    "destinationAccountNumber": "1000000002",
    "amount": 5000.00,
    "description": "Payment for software consulting services"
  }'
```
**Response (200 OK):**
```json
{
  "success": true,
  "message": "Transfer completed successfully",
  "data": {
    "transactionReference": "TX-A1B2C3D4E5F6",
    "sourceAccountNumber": "1000000001",
    "destinationAccountNumber": "1000000002",
    "amount": 5000.00,
    "transactionFee": 25.00,
    "billedAmount": 5025.00,
    "status": "SUCCESSFUL",
    "statusMessage": "Transfer completed successfully",
    "description": "Payment for software consulting services",
    "dateCreated": "2026-08-28T13:30:00"
  },
  "timestamp": "2026-08-28T13:30:00.123"
}
```

### 3. Retrieve Transactions with Filters (`GET /api/v1/transactions`)
```bash
# Filter by status and account number with pagination
curl -X GET "http://localhost:8080/api/v1/transactions?status=SUCCESSFUL&accountNumber=1000000001&page=0&size=10"

# Filter by date range
curl -X GET "http://localhost:8080/api/v1/transactions?startDate=2026-08-01&endDate=2026-08-31"

# Get single transaction by reference
curl -X GET "http://localhost:8080/api/v1/transactions/TX-A1B2C3D4E5F6"
```

### 4. Trigger Commission Analysis (`POST /api/v1/commissions/run-analysis`)
```bash
curl -X POST http://localhost:8080/api/v1/commissions/run-analysis
```
**Response:**
```json
{
  "success": true,
  "message": "Commission analysis executed successfully",
  "data": {
    "totalAnalyzed": 12,
    "commissionWorthyUpdated": 10,
    "totalCommissionCalculated": 150.00,
    "executionTimestamp": "2026-08-28T13:35:00",
    "executionDurationMs": 28
  }
}
```

### 5. Daily Transaction Summary (`GET /api/v1/summaries/daily`)
```bash
# Summary for today
curl -X GET http://localhost:8080/api/v1/summaries/daily

# Summary for a specific past date
curl -X GET "http://localhost:8080/api/v1/summaries/daily?date=2026-08-27"
```
**Response:**
```json
{
  "success": true,
  "message": "Daily summary generated successfully",
  "data": {
    "summaryDate": "2026-08-28",
    "totalTransactions": 25,
    "successfulTransactions": 22,
    "insufficientFundsTransactions": 3,
    "failedTransactions": 0,
    "totalSuccessfulVolume": 250000.00,
    "totalFees": 1250.00,
    "totalCommission": 250.00,
    "statusBreakdown": {
      "SUCCESSFUL": 22,
      "INSUFFICIENT_FUNDS": 3
    }
  }
}
```

---

## 🐳 Running with Docker & Kubernetes

### Docker Compose (App + PostgreSQL)
```bash
docker-compose up --build
```

### Deploying to Kubernetes
```bash
# Apply ConfigMap, Secret, Deployment, Service, and HPA
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml
kubectl apply -f k8s/hpa.yaml
```

---

## 🧪 Automated Testing & Concurrency Verification

Run all test suites via Maven wrapper:
```bash
./mvnw clean test        # Linux / macOS
.\mvnw.cmd clean test    # Windows
```

### Test Coverage Highlights:
* **`FeeCalculatorTest`**: Validates 0.5% fee computation, 100.00 cap boundary, and 20% commission derivation.
* **`TransferServiceTest`**: Validates successful debit/credit, insufficient funds audit logging, account validation, and invalid amounts.
* **`CommissionServiceTest`**: Tests batch evaluation of successful transactions and commission updates.
* **`SummaryServiceTest`**: Tests multi-status metric aggregation for present and past dates.
* **`ConcurrentTransferIntegrationTest`**: Multithreaded integration test executing 20 simultaneous bidirectional transfers across threads to verify zero deadlocks and 100% balance conservation.

---

## 🏦 Pre-seeded Test Accounts

The application automatically seeds test bank accounts on startup:

| Account Number | Account Holder Name | Initial Balance (NGN) | Purpose |
|---|---|---|---|
| `1000000001` | Emmanuel Ugwueze | ₦500,000.00 | Standard high-balance sender |
| `1000000002` | Ekene iloezumma | ₦250,000.00 | Standard recipient/sender |
| `1000000003` | Ikemefuna Nwodo | ₦50.00 | Testing `INSUFFICIENT FUND` transfers |
| `1000000004` | DotLabs Treasury | ₦10,000,000.00 | High-volume corporate account |
