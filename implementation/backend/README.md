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

`/ready` returns `503` until a database adapter is connected.

## Configuration

| Variable | Default | Purpose |
|----------|---------|---------|
| `DATABASE` | `postgres` | Adapter selection (`postgres` or `mongo`) — not wired yet |

## Status

| Done | Pending |
|------|---------|
| Spring Boot scaffold | Demo auth |
| `/v1/health`, `/v1/ready` | Account and payment endpoints |
| | PostgreSQL adapter |
| | MongoDB adapter |
| | Scenario tests (SC-001–SC-015) |

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
