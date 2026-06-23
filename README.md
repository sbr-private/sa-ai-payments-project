# Payments Ledger (ISO 20022)

Specification and reference materials for a minimal double-entry payments ledger. This repository is a **training project**: it defines the problem, acceptance criteria, and database contracts. It does not contain a finished application.

Implementers work from this template. The **use case** (ISO 20022 payments ledger with payer and support demo surfaces) and **database backends** (PostgreSQL and MongoDB) are fixed. Language, framework, project layout, and tooling are not.

Primary intent: [docs/PURPOSE.md](docs/PURPOSE.md)

---

## Repository status

| Present | In progress |
|---------|-------------|
| Behavioural specification ([SPEC.md](docs/SPEC.md)) | Full scenario coverage (SC-001–SC-015) |
| OpenAPI contract ([openapi.yaml](docs/openapi.yaml)) | PostgreSQL adapter |
| 15 acceptance scenarios ([docs/scenarios/](docs/scenarios/)) | Automated scenario runner |
| Reference schemas and seed data for PostgreSQL and MongoDB | Production deployment |
| **Backend implementation** ([implementation/backend/](implementation/backend/)) — Mongo adapter, demo auth, accounts, payments, statements | Frontend UI |

The backend is a work-in-progress Spring Boot API. See [implementation/backend/README.md](implementation/backend/README.md) for build/run instructions and current feature status.

Reference DDL, validators, and seed scripts under `docs/adapters/` may be applied directly to a local database during development. They are illustrative starting points, not a substitute for adapter implementation.

---

## Fixed requirements

These are not open to interpretation:

**Use case.** A realistic payments ledger aligned with ISO 20022 message semantics: account management, credit transfer initiation (pain.001), status reporting (pain.002), and account statements (camt.053). The system must support two demo personas — a corporate payer initiating payments and a support operator investigating them — plus a benchmark path for load testing. Behaviour is defined in [docs/SPEC.md](docs/SPEC.md) and [docs/PURPOSE.md](docs/PURPOSE.md).

**Databases.** Two adapters are required, both implementing the same repository contract against the same behavioural spec:

| Backend | Reference material |
|---------|-------------------|
| PostgreSQL | [SCHEMA.md](docs/adapters/postgres/SCHEMA.md), [schema.sql](docs/adapters/postgres/reference/schema.sql) |
| MongoDB | [SCHEMA.md](docs/adapters/mongodb/SCHEMA.md), [validators and seed](docs/adapters/mongodb/reference/) |

An adapter is compliant when all acceptance scenarios pass and API payloads conform to the OpenAPI schemas. Performance comparison across backends is a secondary deliverable ([SPEC.md §11](docs/SPEC.md#11-comparison-harness)).

---

## Implementation

This section describes what you build. How you build it is your decision.

### Scope

At minimum, deliver:

1. **HTTP API** — REST endpoints per [openapi.yaml](docs/openapi.yaml), backed by a `LedgerRepository` (or equivalent) port with swappable adapters.
2. **PostgreSQL adapter** — persistence using the reference schema or an equivalent design that satisfies the spec.
3. **MongoDB adapter** — persistence using the reference document model or an equivalent design that satisfies the spec.
4. **Scenario suite** — automated tests that execute SC-001 through SC-015 against each adapter.
5. **Demo surfaces** — payer UI, control centre, and a benchmark CLI (or equivalent) as described in [PURPOSE.md](docs/PURPOSE.md).
6. **Resilience suite** — fault-injection tests RT-001 through RT-006 per [resilience/RESILIENCE.md](docs/resilience/RESILIENCE.md) (RT-007 optional).

You may structure this as a monorepo, multiple repositories, a single service with embedded UI, or separate deployable components. The reference architecture below is a suggestion, not a mandate.

### Technology choices

Language, runtime, web framework, ORM/driver, test framework, and frontend stack are **not specified**. Use what fits your experience and constraints.

| Area | Fixed | Open |
|------|-------|------|
| Domain behaviour | [SPEC.md](docs/SPEC.md) | — |
| API shape | [openapi.yaml](docs/openapi.yaml) | Code generation vs hand-written handlers |
| Databases | PostgreSQL, MongoDB | Driver, migration tooling, connection pooling |
| Compliance proof | SC-001–SC-015 | Test runner, CI layout |
| Demo auth | Three identities in [AUTH.md](docs/AUTH.md) | Session vs header-only, UI framework |

**Hints** (optional, not prescriptive):

- A typed language with OpenAPI tooling (e.g. TypeScript, Java with OpenAPI Generator, Python with FastAPI) reduces schema drift.
- Go or Java suit a single-binary API with explicit repository interfaces.
- Scenario tests are often implemented as HTTP integration tests against a running service with ephemeral databases.
- The benchmark CLI should authenticate as `benchmark@demo` and avoid UI login in the hot path ([AUTH.md](docs/AUTH.md)).

### Suggested architecture

```
payer-ui ──┐
control-centre ──┼──► HTTP API ──► LedgerService ──► LedgerRepository
benchmarks ──┘                              ├── PostgresAdapter
                                            └── MongoAdapter
```

| Component | Audience | Purpose |
|-----------|----------|---------|
| `api` | All clients | Ledger service and database selection (`DATABASE=postgres` / `mongo`) |
| `payer-ui` | `payer@demo` | Balance, payment initiation, statement |
| `control-centre` | `support@demo` | Payment lookup, rejection investigation |
| `benchmarks` | `benchmark@demo` | Load generation and adapter comparison |

Adapters may store amounts as minor-unit integers internally. The API must round-trip ISO decimal strings exactly ([MAPPING.md](docs/iso20022/MAPPING.md)).

### Getting started

1. Read [PURPOSE.md](docs/PURPOSE.md) — goals, personas, demo scripts.
2. Read [SPEC.md](docs/SPEC.md) — domain rules, invariants, status codes.
3. Review [SEED.md](docs/SEED.md) and [AUTH.md](docs/AUTH.md) — demo data and login.
4. Study one adapter reference schema (PostgreSQL or MongoDB).
5. Implement the API against one backend; prove compliance with the scenario suite.
6. Implement the second adapter; confirm identical scenario results.
7. Add demo UIs, seed loading, benchmarks, and resilience tests.

To prepare a local database from the reference materials:

```bash
# PostgreSQL
psql "$DATABASE_URL" \
  -f docs/adapters/postgres/reference/schema.sql \
  -f docs/adapters/postgres/reference/examples/seed.sql

# MongoDB (from project root)
mongosh "$MONGODB_URI" --file docs/adapters/mongodb/reference/examples/seed.js
```

Demo seed data is for UI and manual testing only. Acceptance scenarios define their own setup and do not depend on seed data ([docs/scenarios/README.md](docs/scenarios/README.md)).

---

## ISO 20022 API

| Message | Role | Endpoint |
|---------|------|----------|
| pain.001 | Initiate credit transfer | `POST /v1/payment-initiations` |
| pain.002 | Payment status | Initiation response, `GET /v1/payment-initiations/transactions/{endToEndId}` |
| camt.053 | Account statement | `GET /v1/accounts/{id}/statements` |

XSD targets: pain.001.001.09, pain.002.001.10, camt.053.001.08.

Amounts use decimal strings, not JSON floats:

```json
{ "value": "50.00", "ccy": "USD" }
```

Idempotency key: `PmtId.endToEndId` (max 35 characters).

Field mapping: [docs/iso20022/MAPPING.md](docs/iso20022/MAPPING.md)

---

## Demo login

Three hardcoded identities. Password for all: `demo`

| Email | Role | Used by |
|-------|------|---------|
| `payer@demo` | payer | Payer portal — Acme Corp account |
| `support@demo` | support | Control centre — any payment or account |
| `benchmark@demo` | benchmark | Benchmark CLI — load tests only |

`POST /auth/login` returns a user object. Subsequent API calls send `X-Demo-User: <email>`. Users are application configuration, not ledger database records.

Details: [docs/AUTH.md](docs/AUTH.md) · Fixture: [docs/fixtures/seed-users.json](docs/fixtures/seed-users.json)

---

## Demo seed data

Pre-loaded ledger state for UI development and manual demos. Not used by compliance scenarios SC-001–SC-015.

| Step | Event | EndToEndId | Status |
|------|-------|------------|--------|
| Opening | Settlement → Acme $1,000 | `E2E-SEED-0001` | ACSC |
| Success | Acme → Supplier $50 | `E2E-INV-2024-0558` | ACSC |
| Failed | Acme → Supplier $2,000 attempt | `E2E-INV-2024-0999` | RJCT / AM04 |

Resulting balances: Acme $950.00, Supplier $50.00.

Support demo lookup: `GET /v1/payment-initiations/transactions/E2E-INV-2024-0999` — rejected for insufficient funds ($2,000 instructed against $950 balance).

| Format | File |
|--------|------|
| Narrative and UUIDs | [docs/SEED.md](docs/SEED.md) |
| JSON | [docs/adapters/mongodb/reference/examples/seed.json](docs/adapters/mongodb/reference/examples/seed.json) |
| SQL | [docs/adapters/postgres/reference/examples/seed.sql](docs/adapters/postgres/reference/examples/seed.sql) |

---

## Compliance testing

An adapter is spec-compliant when:

1. All 15 acceptance scenarios pass ([docs/scenarios/](docs/scenarios/))
2. API payloads validate against [openapi.yaml](docs/openapi.yaml)
3. Domain invariants in [SPEC.md](docs/SPEC.md) hold after every operation

Scenarios authenticate with `X-Demo-User: benchmark@demo` or equivalent test setup. Auth is not part of pass/fail criteria. Scenarios, performance benchmarks, and resilience tests exercise the **HTTP API** for verification — see [scenarios/README.md](docs/scenarios/README.md#test-boundary), [benchmarks/PERFORMANCE.md](docs/benchmarks/PERFORMANCE.md#test-boundary), and [resilience/README.md](docs/resilience/README.md#test-boundary).

| ID | Title |
|----|-------|
| SC-001 | [Register account](docs/scenarios/SC-001-create-account.md) |
| SC-002 | [Credit transfer (pain.001)](docs/scenarios/SC-002-transfer.md) |
| SC-003 | [Insufficient funds (AM04)](docs/scenarios/SC-003-insufficient-funds.md) |
| SC-004 | [EndToEndId replay](docs/scenarios/SC-004-idempotent-replay.md) |
| SC-005 | [EndToEndId conflict (DU04)](docs/scenarios/SC-005-idempotency-conflict.md) |
| SC-006 | [Currency mismatch (CURR)](docs/scenarios/SC-006-currency-mismatch.md) |
| SC-007 | [Closed account (AC04)](docs/scenarios/SC-007-closed-account.md) |
| SC-008 | [Double-entry](docs/scenarios/SC-008-double-entry.md) |
| SC-009 | [Balance integrity](docs/scenarios/SC-009-balance-integrity.md) |
| SC-010 | [Concurrent transfers](docs/scenarios/SC-010-concurrent-transfers.md) |
| SC-011 | [Statement pagination](docs/scenarios/SC-011-pagination.md) |
| SC-012 | [Transaction not found](docs/scenarios/SC-012-transfer-not-found.md) |
| SC-013 | [Account not found](docs/scenarios/SC-013-account-not-found.md) |
| SC-014 | [Invalid amount (AM12)](docs/scenarios/SC-014-amount-validation.md) |
| SC-015 | [Self-transfer (AG01)](docs/scenarios/SC-015-self-transfer.md) |

---

## Documentation map

| Document | Purpose |
|----------|---------|
| [docs/PURPOSE.md](docs/PURPOSE.md) | Goals, personas, demo scripts |
| [docs/SPEC.md](docs/SPEC.md) | Canonical behaviour (v2.1) |
| [docs/SEED.md](docs/SEED.md) | Demo seed narrative and fixed UUIDs |
| [docs/AUTH.md](docs/AUTH.md) | Demo login |
| [docs/iso20022/MAPPING.md](docs/iso20022/MAPPING.md) | ISO XML to JSON, minor-unit conversion |
| [docs/openapi.yaml](docs/openapi.yaml) | API contract |
| [docs/scenarios/](docs/scenarios/) | Acceptance scenarios |
| [docs/benchmarks/PERFORMANCE.md](docs/benchmarks/PERFORMANCE.md) | Performance benchmark specification |
| [docs/resilience/](docs/resilience/) | Resilience tests RT-001–RT-007 |
| [docs/fixtures/](docs/fixtures/) | Golden JSON examples |
| [docs/adapters/](docs/adapters/) | PostgreSQL and MongoDB reference schemas |

Reading order: PURPOSE → SPEC → SEED and AUTH → MAPPING and openapi.yaml → adapter schema → scenarios.

---

## License

MIT
