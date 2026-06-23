# Resilience Tests

Fault-injection tests for database adapters under process and infrastructure failure. Each test is **Given / When / Then** against the running [HTTP API](../openapi.yaml). Verification uses API calls and domain invariants from [SPEC.md](../SPEC.md) — not direct database inspection as the primary pass/fail mechanism.

**Index:** RT-001 through RT-007 — see [SPEC Appendix D](../SPEC.md#appendix-d-resilience-test-index).

Full harness guidance: [RESILIENCE.md](./RESILIENCE.md)

---

## How resilience tests relate to other suites

| Suite | Question | Fault injection |
|-------|----------|-----------------|
| [scenarios/](../scenarios/) SC-001–SC-015 | Does behaviour match the spec? | No |
| [benchmarks/PERFORMANCE.md](../benchmarks/PERFORMANCE.md) | How fast under load? | No |
| **resilience/** RT-001–RT-007 | What breaks, and is data still correct? | Yes |

Resilience tests assume SC-001–SC-015 are already green on the adapter under test.

---

## Test boundary

Faults are injected at the **process or infrastructure** layer (kill API, stop database, block network). **Assertions** are made through the HTTP API:

```
fault injection ──► API / database / network
                         │
scenario runner ──HTTP──► Payments API ──► verify balances, status, invariants
```

Repository unit tests that simulate failures without a running API do not satisfy RT-001–RT-007.

Optional: after API-level checks pass, implementers may inspect the database for debugging. That inspection is not the compliance gate.

---

## Authentication

Same as scenarios: `X-Demo-User: benchmark@demo` on all requests. See [AUTH.md](../AUTH.md).

---

## Running (planned)

```bash
# Illustrative — implementation TBD
npm run test:resilience -- --adapter=postgres
npm run test:resilience -- --adapter=mongo --include=optional
```

RT-007 (database failover) is **optional** — requires a replica set or PostgreSQL HA setup. Document skip reason if the environment cannot support it.

Pass criteria: required tests (RT-001–RT-006) documented per adapter; not gated for v1 correctness compliance. See [RESILIENCE.md](./RESILIENCE.md#pass-criteria).
