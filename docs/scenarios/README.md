# Acceptance Scenarios

Behavioural compliance tests for database adapters. Each scenario is **Given / When / Then** against the [SPEC](../SPEC.md) and [openapi.yaml](../openapi.yaml).

**Index:** SC-001 through SC-015 — see [SPEC Appendix A](../SPEC.md#appendix-a-scenario-index) or the [project README](../../README.md#scenario-index).

---

## Test boundary

Scenarios run **through the HTTP API**, not by calling the database or repository directly.

```
scenario runner ──HTTP──► Payments API ──► LedgerRepository ──► PostgreSQL / MongoDB
```

Each scenario describes REST requests and expected JSON responses. Setup steps (create account, fund balance, close account) are also issued via the API unless the spec explicitly allows a test-only helper endpoint. The goal is to prove that the **application** behaves identically on both adapters — including validation, idempotency, double-entry, and concurrency — not that a hand-written SQL or aggregation script works in isolation.

Repository unit tests are useful during development but **do not** satisfy SC-001–SC-015. A scenario passes only when the running API returns the expected result for the given adapter.

---

## How scenarios relate to other docs

| Concern | Document | Notes |
|---------|----------|-------|
| **Why we test** | [PURPOSE.md](../PURPOSE.md) | DB comparison mission |
| **Expected behaviour** | [SPEC.md](../SPEC.md) | Source of truth |
| **Demo seed data** | [SEED.md](../SEED.md) | Separate — for UI demos only |
| **Golden JSON** | [fixtures/](../fixtures/) | Example request/response shapes |
| **Performance** | [benchmarks/PERFORMANCE.md](../benchmarks/PERFORMANCE.md) | API-level load tests (same boundary) |
| **Resilience** | [resilience/](../resilience/) | Fault injection; API-level verification |

Scenarios **do not** load [demo seed](../SEED.md). Each scenario defines its own setup (often via test helpers for balances, closed accounts, etc.).

---

## Authentication

Scenarios are **auth-agnostic** at the HTTP layer:

- Send `X-Demo-User: benchmark@demo` on all requests, **or**
- Use a test harness that bypasses demo auth middleware

Auth is not part of compliance pass/fail. See [AUTH.md](../AUTH.md).

---

## Running (planned)

```bash
# Illustrative — implementation TBD
npm run test:scenarios -- --adapter=postgres
npm run test:scenarios -- --adapter=mongo
```

Pass criteria: 100% green on every adapter before benchmarking.
