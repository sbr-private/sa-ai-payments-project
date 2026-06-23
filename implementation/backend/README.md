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

`/health` and `/auth/login` are public. All other routes require `X-Demo-User: <email>` (e.g. `benchmark@demo`).

`/ready` returns `503` when the database is unreachable.

## Demo flow

Smoke test for health, demo auth, readiness, and account registration (SC-001):

```bash
cd implementation/backend
chmod +x scripts/smoke-auth-and-register-account.sh   # once
./scripts/smoke-auth-and-register-account.sh
```

The script walks through:

1. `GET /health` — liveness (no auth)
2. `POST /auth/login` — payer login
3. `GET /ready` — database check (`X-Demo-User: benchmark@demo`)
4. `POST /accounts` — register a USD account (SC-001)
5. `POST /auth/login` — support login

Override the base URL or demo user if needed:

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

## Design

### Layering

```
HTTP (controllers, ISO JSON DTOs)
        → LedgerService (business rules, pain.002 mapping)
        → LedgerRepository (port)
        → MongoAdapter | PostgresAdapter
```

Controllers and `LedgerService` must not depend on JDBC or Mongo driver types. Adapters own transactions, locking, and schema mapping.

### Mongo first, PostgreSQL fast follow

Default adapter is **MongoDB**. The PostgreSQL adapter is a fast follow — but the `LedgerRepository` port is designed for both from day one. No Mongo-specific assumptions in domain code; settlement atomicity and locking live inside each adapter.

### MongoDB transactions

Settlement (coming later) uses multi-document transactions. That requires a **replica set** — a standalone local `mongod` is fine for account registration and the current demo flow.

When payment initiation is implemented, either configure a [single-node replica set](https://www.mongodb.com/docs/manual/tutorial/deploy-replica-set/) locally or use Atlas.

### Testing

Unit tests cover domain and service logic. No Testcontainers or adapter integration tests. Compliance is proven later via HTTP scenario tests (SC-001–SC-015) against a running app.

### Test helpers

Several acceptance scenarios fund balances and close accounts via test-only API endpoints (`ENABLE_TEST_HELPERS=true`). There is no public v1 API for those operations.

## Status

| Done | Pending |
|------|---------|
| Spring Boot scaffold | Payment initiation |
| `/v1/health`, `/v1/ready` | PostgreSQL adapter operations (fast follow) |
| Demo auth (`POST /auth/login`, `X-Demo-User`) | Scenario tests (SC-001–SC-015) |
| `POST /accounts`, `GET /accounts/{id}` — Mongo | |

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
