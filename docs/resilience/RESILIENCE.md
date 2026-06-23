# Resilience Test Specification

Fault-injection tests for PostgreSQL and MongoDB adapters. Defines **what failures to simulate** and **what must remain true** afterward.

Implementers build the harness under `implementation/resilience/` (or combine with `implementation/benchmarks/`). Individual test cases: [RT-001](./RT-001-api-process-kill.md) through [RT-007](./RT-007-database-failover.md).

**Related:**

| Document | Role |
|----------|------|
| [SPEC.md §12](../SPEC.md#12-resilience-testing) | Summary in the main spec |
| [scenarios/](../scenarios/) | Correctness prerequisites |
| [benchmarks/PERFORMANCE.md](../benchmarks/PERFORMANCE.md) | Load tests (no fault injection) |

---

## Purpose

Correctness scenarios prove happy-path and validation behaviour. Resilience tests prove that **atomic settlement** and **balance integrity** survive real failures: API crashes, database restarts, network loss, and client retries after timeouts.

The comparison question for this project: when something fails, does the PostgreSQL adapter behave the same as the MongoDB adapter, and is money neither created nor destroyed?

---

## Prerequisites

1. SC-001–SC-015 pass on the adapter ([scenarios/README.md](../scenarios/README.md)).
2. API and database run locally (typically Docker Compose or equivalent) so processes can be stopped and restarted by the harness.
3. Harness can capture account balances and `endToEndId` status **before** fault injection and **after** recovery.
4. Recommended: expose `GET /ready` returning `200` when the database is reachable and `503` when it is not. `GET /health` may remain a shallow liveness check ([SPEC.md OP-006](../SPEC.md#op-006-health-check)).

---

## Invariants (always)

After every resilience test, these must hold (verify via API unless noted):

| Invariant | Spec reference |
|-----------|----------------|
| No partial settlement | Debit and credit move together, or neither moves |
| Double-entry | Every `ACSC` has exactly one `DBIT` and one `CRDT` of equal magnitude |
| Balance non-negative | No account has `bal.value` < 0 |
| Idempotency | Same `endToEndId` + same body → same outcome; no duplicate settlement |
| Global balance | Sum of all account balances unchanged except for intentional test funding |

---

## Test index

| ID | Title | Required |
|----|-------|----------|
| RT-001 | [API process killed during transfer](./RT-001-api-process-kill.md) | Yes |
| RT-002 | [Database unavailable during transfer](./RT-002-database-unavailable.md) | Yes |
| RT-003 | [Database restart and recovery](./RT-003-database-restart.md) | Yes |
| RT-004 | [Client timeout and idempotent retry](./RT-004-client-timeout-retry.md) | Yes |
| RT-005 | [API restart under concurrent load](./RT-005-api-restart-concurrent.md) | Yes |
| RT-006 | [Read paths during database outage](./RT-006-read-during-outage.md) | Yes |
| RT-007 | [Database node failover](./RT-007-database-failover.md) | Optional |

---

## Harness design

### Location

```
implementation/resilience/     ← fault injection scripts and test runner
docs/resilience/               ← This specification
```

### Fault injection

The harness controls infrastructure, not application internals:

| Mechanism | Examples |
|-----------|----------|
| Process | `SIGKILL` on API PID, `docker stop` on API container |
| Database | `docker stop` on postgres/mongod, `pg_ctl stop`, brief firewall rule |
| Network | Disconnect API container from DB network namespace |
| Client | Short HTTP timeout + retry with same `endToEndId` |

Avoid modifying adapter source code to trigger faults. Tests should reflect operational reality.

### Verification flow

1. **Baseline** — record balances for accounts under test.
2. **Action** — submit transfer(s); inject fault at defined point.
3. **Recovery** — restart failed component(s); wait until `GET /ready` returns `200` (or API accepts traffic).
4. **Assert** — query status by `endToEndId`; re-read balances; check invariants.
5. **Record** — write result artifact (see below).

### Result format

```
implementation/resilience/results/<adapter>/<test-id>-<YYYYMMDD-HHMMSS>.json
```

Illustrative fields: `test_id`, `adapter`, `fault_type`, `outcome` (`pass` / `fail`), `end_to_end_id`, `tx_sts_after_recovery`, `balances_before`, `balances_after`, `notes`.

---

## Pass criteria

| Check | Required for training completion |
|-------|----------------------------------|
| RT-001–RT-006 pass per adapter | Recommended |
| RT-007 pass or documented skip | Optional |
| Written comparison note (Postgres vs Mongo behaviour) | Recommended |

Resilience is **not** gated for v1 correctness compliance (unlike SC-001–SC-015). Failures here are learning outcomes — document what happened and whether invariants held.

---

## Fair comparison rules

1. Same fault type and timing relative to the request lifecycle.
2. Same API build; only `DATABASE` changes between adapter runs.
3. Document whether MongoDB used a single node or replica set, and PostgreSQL used a single instance or HA pair.
4. Do not compare a single-node Mongo result against a Postgres Patroni cluster without noting the asymmetry.

---

## Out of scope (v1)

- Multi-AZ cloud chaos (AWS AZ failure simulation)
- Kubernetes pod anti-affinity and rolling deploys
- Split-brain write acceptance on both database primaries
- Long-duration partition (hours)
- Resilience of payer UI or control centre (API only)

These may be added in a later revision.
