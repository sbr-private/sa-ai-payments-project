# Acceptance Scenarios

Behavioural compliance tests for database adapters. Each scenario is **Given / When / Then** against the [SPEC](../SPEC.md) and [openapi.yaml](../openapi.yaml).

**Index:** SC-001 through SC-015 — see [SPEC Appendix A](../SPEC.md#appendix-a-scenario-index) or the [project README](../../README.md#scenario-index).

---

## How scenarios relate to other docs

| Concern | Document | Notes |
|---------|----------|-------|
| **Why we test** | [PURPOSE.md](../PURPOSE.md) | DB comparison mission |
| **Expected behaviour** | [SPEC.md](../SPEC.md) | Source of truth |
| **Demo seed data** | [SEED.md](../SEED.md) | Separate — for UI demos only |
| **Golden JSON** | [fixtures/](../fixtures/) | Example request/response shapes |

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
