# Payments Ledger — Backend

Java 21 / Spring Boot HTTP API for the ISO 20022 payments ledger.

Spec: [docs/SPEC.md](../../docs/SPEC.md) · OpenAPI: [docs/openapi.yaml](../../docs/openapi.yaml)

Frontend will live in a separate folder under `implementation/`.

## Prerequisites

- JDK 21
- Maven 3.9+
- MongoDB on `localhost:27017` (local dev default)

### Install (macOS / Homebrew)

```bash
brew install openjdk@21 maven mongodb-community@7.0
brew services start mongodb-community@7.0
sudo ln -sfn "$(brew --prefix)/opt/openjdk@21/libexec/openjdk.jdk" /Library/Java/JavaVirtualMachines/openjdk-21.jdk
```

Or run MongoDB in Docker:

```bash
docker run -d --name ledger-mongo -p 27017:27017 mongo:7
```

Add to `~/.zshrc`:

```bash
export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
export PATH="$JAVA_HOME/bin:$PATH"
```

Homebrew's `maven` formula may pull in a newer JDK (e.g. 26). Setting `JAVA_HOME` pins Maven to 21. Verify:

```bash
java -version   # 21.x
mvn -version    # Java version: 21.x
```

## Build and test

```bash
cd implementation/backend
mvn clean install
```

## Run

Start MongoDB, then the API. The default connection is `mongodb://localhost:27017/ledger` — no `MONGODB_URI` required for local dev.

For test helpers (credit/close accounts) and the full SC-002 smoke flow:

```bash
ENABLE_TEST_HELPERS=true mvn spring-boot:run
```

For the payer/support demo dataset (Acme $950, Supplier $50, failed payment `E2E-INV-2024-0999`):

```bash
LOAD_SAMPLE_DATA=true mvn spring-boot:run
```

`LOAD_SAMPLE_DATA` is idempotent — skipped when the Acme account already exists.

```bash
# if using Homebrew MongoDB
brew services start mongodb-community@7.0

cd implementation/backend
mvn spring-boot:run
```

| Endpoint | URL |
|----------|-----|
| Liveness | `GET http://localhost:8080/v1/health` |
| Readiness | `GET http://localhost:8080/v1/ready` |
| Login | `POST http://localhost:8080/v1/auth/login` |
| Register account | `POST http://localhost:8080/v1/accounts` |
| Get account | `GET http://localhost:8080/v1/accounts/{id}` |
| Credit account (test) | `POST http://localhost:8080/v1/test/accounts/{id}/credit` |
| Close account (test) | `POST http://localhost:8080/v1/test/accounts/{id}/close` |
| Initiate payment | `POST http://localhost:8080/v1/payment-initiations` |
| Transaction status | `GET http://localhost:8080/v1/payment-initiations/transactions/{endToEndId}` |
| Account statement | `GET http://localhost:8080/v1/accounts/{id}/statements` |

`/health` and `/auth/login` are public. All other routes require `X-Demo-User: <email>` (e.g. `benchmark@demo`).

`/ready` returns `503` when the database is unreachable.

## Demo flow

Smoke test for health, demo auth, readiness, account registration (SC-001), test-helper funding, payment initiation (SC-002), and statements:

```bash
cd implementation/backend
chmod +x scripts/smoke-auth-and-register-account.sh   # once
ENABLE_TEST_HELPERS=true mvn spring-boot:run          # in another terminal
./scripts/smoke-auth-and-register-account.sh
```

The script walks through:

1. `GET /health` — liveness (no auth)
2. `POST /auth/login` — payer login
3. `GET /ready` — database check (`X-Demo-User: benchmark@demo`)
4. `POST /accounts` — register a USD account (SC-001)
5. `POST /auth/login` — support login
6. `GET /accounts/{id}` — fetch registered account
7. `POST /test/accounts/{id}/credit` — fund $1,000 (test helper)
8. `POST /accounts` — register creditor account
9. `POST /payment-initiations` — $50 transfer (SC-002, pain.001 → pain.002)
10. `GET /accounts/{id}` — verify balances (950 / 50)
11. `GET /accounts/{id}/statements` — camt.053 entries (DBIT / CRDT)

Start the server with `ENABLE_TEST_HELPERS=true` for steps 7–11. Override the base URL or demo user if needed:

```bash
BASE_URL=http://localhost:8080/v1 DEMO_USER=benchmark@demo ./scripts/smoke-auth-and-register-account.sh
```

`jq` is optional; responses are pretty-printed when it is installed.

### Manual curl (single steps)

```bash
# Health
curl -s http://localhost:8080/v1/health

# Login
curl -s -X POST http://localhost:8080/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"payer@demo","password":"demo"}'

# Register account (needs MongoDB + header)
curl -s -X POST http://localhost:8080/v1/accounts \
  -H 'Content-Type: application/json' \
  -H 'X-Demo-User: benchmark@demo' \
  -d '{"owner":{"nm":"Acme Corp","id":{"othr":{"id":"user_123"}}},"ccy":"USD"}'
```

Demo users (password `demo`): `payer@demo`, `support@demo`, `benchmark@demo`.

## Configuration

| Variable | Default | Purpose |
|----------|---------|---------|
| `DATABASE` | `mongo` | Adapter selection (`mongo` or `postgres`) |
| `MONGODB_URI` | `mongodb://localhost:27017/ledger` | MongoDB connection string |
| `DATABASE_URL` | — | PostgreSQL JDBC URL (when `DATABASE=postgres`) |
| `ENABLE_TEST_HELPERS` | `false` | Test-only credit/close account endpoints |
| `LOAD_SAMPLE_DATA` | `false` | Load minimal demo seed on startup (see [SEED.md](../../docs/SEED.md)) |

## Design

### Layering

```
HTTP (controllers, ISO JSON DTOs)
        → domain services (AccountService, PaymentService, …)
        → LedgerRepository (port)
        → MongoAdapter | PostgresAdapter
```

Controllers and domain services must not depend on JDBC or Mongo driver types. Adapters own transactions, locking, and schema mapping.

### Mongo first, PostgreSQL fast follow

Default adapter is **MongoDB**. The PostgreSQL adapter is a fast follow — but the `LedgerRepository` port is designed for both from day one. No Mongo-specific assumptions in domain code; settlement atomicity and locking live inside each adapter.

### MongoDB settlement (local dev)

Settlement uses conditional `findAndModify` on account balances — a standalone local `mongod` is sufficient for the current demo flow. A replica set (or Atlas) will be required if we move to multi-document transactions in the adapter.

### Testing

Unit tests cover domain and service logic. No Testcontainers or adapter integration tests. Compliance is proven later via HTTP scenario tests (SC-001–SC-015) against a running app.

### Test helpers

Several acceptance scenarios fund balances and close accounts via test-only API endpoints (`ENABLE_TEST_HELPERS=true`). There is no public v1 API for those operations.

## Status

| Done | Pending |
|------|---------|
| Spring Boot scaffold | PostgreSQL adapter operations (fast follow) |
| `/v1/health`, `/v1/ready` | Automated scenario runner (SC-001–SC-015) |
| Demo auth (`POST /auth/login`, `X-Demo-User`) | |
| `POST /accounts`, `GET /accounts/{id}` — Mongo | |
| Test helpers — `POST /test/accounts/{id}/credit`, `/close` | |
| `POST /payment-initiations` — Mongo (SC-002 happy path) | |
| `GET /accounts/{id}/statements` — Mongo with pagination (SC-011) | |
| Sample data loader (`LOAD_SAMPLE_DATA=true`) | |
| Rejection paths — AM04, AC04, AG01, AM12, CURR, BE01 | |
| Idempotency replay / conflict — SC-004, SC-005 | |
| `GET /payment-initiations/transactions/{endToEndId}` | |
| Concurrent settlement — SC-010 | |

## Package layout (planned)

```
com.payments.ledger
├── api/           REST controllers and DTOs
├── domain/        Ledger service and business rules
├── repository/    LedgerRepository port
└── adapter/
    ├── postgres/
    └── mongo/
```
