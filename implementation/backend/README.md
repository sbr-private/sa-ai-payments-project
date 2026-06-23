# Payments Ledger — Backend

Java 21 / Spring Boot HTTP API for the ISO 20022 payments ledger.

Spec: [docs/SPEC.md](../../docs/SPEC.md) · OpenAPI: [docs/openapi.yaml](../../docs/openapi.yaml)

Frontend will live in a separate folder under `implementation/`.

## Prerequisites

- JDK 21
- Maven 3.9+

### Install (macOS / Homebrew)

```bash
brew install openjdk@21 maven
sudo ln -sfn "$(brew --prefix)/opt/openjdk@21/libexec/openjdk.jdk" /Library/Java/JavaVirtualMachines/openjdk-21.jdk
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

```bash
mvn spring-boot:run
```

| Endpoint | URL |
|----------|-----|
| Liveness | `GET http://localhost:8080/v1/health` |
| Readiness | `GET http://localhost:8080/v1/ready` |
| Login | `POST http://localhost:8080/v1/auth/login` |

`/health` and `/auth/login` are public. All other routes require `X-Demo-User: <email>` (e.g. `benchmark@demo`).

`/ready` returns `503` when the database is unreachable.

## Configuration

| Variable | Default | Purpose |
|----------|---------|---------|
| `DATABASE` | `mongo` | Adapter selection (`mongo` or `postgres`) |
| `MONGODB_URI` | — | MongoDB connection string (Atlas or local replica set) |
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

Settlement uses multi-document transactions (lock accounts, insert payment, book statement entries). That requires a **replica set** — a standalone `mongod` is not sufficient.

We expect to run against **MongoDB Atlas**, which provides a replica set by default. If you use local MongoDB instead, configure a single-node replica set.

### Testing

Unit tests cover domain and service logic. No Testcontainers or adapter integration tests. Compliance is proven later via HTTP scenario tests (SC-001–SC-015) against a running app.

### Test helpers

Several acceptance scenarios fund balances and close accounts via test-only API endpoints (`ENABLE_TEST_HELPERS=true`). There is no public v1 API for those operations.

## Status

| Done | Pending |
|------|---------|
| Spring Boot scaffold | Account and payment endpoints |
| `/v1/health`, `/v1/ready` | Mongo adapter operations |
| Demo auth (`POST /auth/login`, `X-Demo-User`) | PostgreSQL adapter operations (fast follow) |
| `LedgerRepository` port + domain models | Scenario tests (SC-001–SC-015) |
| Runtime adapter selection (`mongo` / `postgres`) | |
| Mongo / Postgres `isHealthy()` stubs | |

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
