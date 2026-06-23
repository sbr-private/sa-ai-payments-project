# Performance Benchmark Specification

Load and comparison tests for database adapters. This document defines **what to measure** and **how to run it**. Implementers build the harness under `implementation/benchmarks/` (or equivalent); this file is the contract.

**Related:**

| Document | Role |
|----------|------|
| [SPEC.md §11](../SPEC.md#11-comparison-harness) | Summary reference in the main spec |
| [scenarios/](../scenarios/) | Correctness tests (must pass before timed runs) |
| [AUTH.md](../AUTH.md) | `benchmark@demo` service identity |
| [PURPOSE.md](../PURPOSE.md) | Why performance comparison matters |

---

## Purpose

Compare PostgreSQL and MongoDB adapters under a realistic payments workload. The same API, auth model, and request shapes are used for both backends. Results are recorded per adapter so differences in write throughput, read latency, and storage footprint can be discussed with evidence.

Performance benchmarks are **secondary** to correctness. They do not gate v1 compliance, but both adapters must produce recorded results on a documented hardware profile before the training project is considered complete ([PURPOSE.md](../PURPOSE.md)).

---

## Test boundary

All timed benchmarks run **against the HTTP API**, not directly against the database.

```
benchmark CLI ──HTTP──► Payments API ──► LedgerRepository ──► PostgreSQL / MongoDB
```

The harness issues the same REST requests that the payer UI, control centre, and correctness scenarios use ([openapi.yaml](../openapi.yaml)). Latency and TPS include serialization, service logic, connection pooling, and persistence. That is intentional: the project compares databases **in the context of a realistic application**, not as isolated driver micro-benchmarks.

**Do not** use `psql`, `mongosh`, or repository unit tests as the primary performance harness. Direct database scripts bypass idempotency, double-entry settlement, and concurrency rules defined in [SPEC.md](../SPEC.md). They are not comparable across adapters and do not reflect demo or production call paths.

Repository-level benchmarks are optional diagnostics (e.g. isolating slow queries). They are out of scope for v1 pass criteria and must not replace API-level suites.

---

## Prerequisites

1. **Correctness green** — SC-001 through SC-015 pass on the adapter under test ([scenarios/README.md](../scenarios/README.md)).
2. **API running** — single instance, known version/commit, `DATABASE` env set to `postgres` or `mongo`.
3. **Isolated database** — dedicated instance or schema; not shared with other developers or CI jobs during a timed run.
4. **Warm-up** — one untimed suite iteration (or 60 seconds of load) before measurement; discard results.

Do not run timed benchmarks against demo seed data used for UI walkthroughs ([SEED.md](../SEED.md)). Use the **seed profile** suite (below) or a harness-managed dataset.

---

## Harness

### Location

```
implementation/benchmarks/     ← CLI and load scripts (your code)
docs/benchmarks/               ← This specification
```

### Authentication

The harness acts as `benchmark@demo`. Set on every request:

```
X-Demo-User: benchmark@demo
```

No `POST /auth/login` in the measurement window. See [AUTH.md](../AUTH.md).

### Invocation (illustrative)

```bash
# From implementation/benchmarks/ after you build it
./benchmarks run --adapter=postgres --suite=write-heavy --duration=300s --concurrency=32
./benchmarks run --adapter=mongo --suite=mixed --duration=300s --concurrency=32
./benchmarks compare --adapters=postgres,mongo --suite=read-heavy
```

Flag names and CLI layout are implementation-defined. Required inputs: **adapter**, **suite**, **duration** (or request count), **concurrency**.

### Load generator

Use concurrent HTTP clients against the REST API ([openapi.yaml](../openapi.yaml)). Acceptable tools include k6, vegeta, wrk, or a small custom runner. The generator must:

- Issue unique `PmtId.endToEndId` values per initiation (prefix + sequence; max 35 chars).
- Use pre-created account IDs from the seed profile or suite setup.
- Record per-request latency, HTTP status, and response body status (`txSts`) where applicable.
- Not reuse idempotency keys across concurrent workers unless testing replay (not part of perf suites).

---

## Suites

| Suite | Traffic mix | Primary question |
|-------|-------------|------------------|
| `correctness` | — | Runs SC-001–SC-015; not timed |
| `write-heavy` | 100% payment initiations | Settlement TPS under write pressure |
| `read-heavy` | 80% reads, 20% writes | Support-style lookup and statement patterns |
| `mixed` | 50% reads, 50% writes | Combined operational load |
| `seed-profile` | Bulk setup only | Time and storage to reach scale target |

### `write-heavy`

| Parameter | Value |
|-----------|-------|
| Duration | 5 minutes (timed) |
| Concurrency | 32 workers (default; document if changed) |
| Endpoints | `POST /v1/payment-initiations` only |

**Setup:** At least 100 active debtor accounts (USD, funded $10,000.00 each) and 100 creditor accounts. Rotate debtor/creditor pairs across workers.

**Request body:** pain.001 per [fixtures/pain001-initiation.json](../fixtures/pain001-initiation.json). Instructed amount: `$1.00` USD. Unique `endToEndId` per request.

**Success criterion for a request:** HTTP 2xx and `txSts` = `ACSC`.

### `read-heavy`

| Parameter | Value |
|-----------|-------|
| Duration | 5 minutes (timed) |
| Concurrency | 32 workers |
| Mix | 40% `GET /v1/accounts/{id}`, 40% `GET /v1/accounts/{id}/statements`, 20% `POST /v1/payment-initiations` |

**Setup:** Seed profile completed (below) or existing dataset with ≥ 10,000 accounts and ≥ 100,000 settled transfers. Read workers sample account IDs uniformly from the funded set.

**Write slice:** Same as write-heavy ($1.00 transfers, unique `endToEndId`).

### `mixed`

| Parameter | Value |
|-----------|-------|
| Duration | 5 minutes (timed) |
| Concurrency | 32 workers |
| Mix | 50% `POST /v1/payment-initiations`, 25% `GET /v1/accounts/{id}`, 25% `GET /v1/payment-initiations/transactions/{endToEndId}` |

**Setup:** Seed profile or equivalent. Status lookups use `endToEndId` values from prior settled transfers in the dataset.

### `seed-profile`

Bulk setup **before** read-heavy or mixed timed runs. Not included in latency percentiles.

| Step | Target |
|------|--------|
| Create accounts | 10,000 active USD accounts |
| Fund accounts | $10,000.00 each (via internal settlement account or harness credit) |
| Settled transfers | 1,000,000 `ACSC` payment initiations |

Record:

- Wall-clock time for the full seed
- Final row/document counts per collection/table
- On-disk size (PostgreSQL: `pg_database_size`; MongoDB: `db.stats().dataSize` + index size)

Document hardware and adapter version alongside these numbers.

---

## Metrics

Collect per suite, per adapter, per endpoint (where applicable):

| Metric | Description |
|--------|-------------|
| `requests_total` | Completed HTTP requests |
| `requests_per_second` | Mean TPS over timed window |
| `latency_ms_p50` | 50th percentile end-to-end latency |
| `latency_ms_p95` | 95th percentile |
| `latency_ms_p99` | 99th percentile |
| `error_rate` | Non-2xx HTTP responses / total |
| `reject_rate` | HTTP 2xx with `txSts` = `RJCT` / total initiations |
| `conflict_rate` | HTTP 409 or `DU04` / total initiations (if exposed) |

Optional but recommended:

- CPU and memory utilization of API process and database during the run
- Connection pool saturation (active / max)
- Storage size after `seed-profile`

---

## Result format

Write one JSON file per run:

```
implementation/benchmarks/results/<adapter>/<suite>-<YYYYMMDD-HHMMSS>.json
```

`results/` should be gitignored; commit a **sample** or summary to docs only if useful for teaching.

### Schema (illustrative)

```json
{
  "run_id": "20260623-143022-postgres-write-heavy",
  "adapter": "postgres",
  "suite": "write-heavy",
  "api_version": "git:abc1234",
  "environment": {
    "host_cpu": "Apple M2 Pro",
    "host_memory_gb": 32,
    "database_version": "PostgreSQL 16.2",
    "api_instances": 1,
    "concurrency": 32,
    "duration_seconds": 300
  },
  "summary": {
    "requests_total": 45210,
    "requests_per_second": 150.7,
    "error_rate": 0.0,
    "reject_rate": 0.0
  },
  "endpoints": {
    "POST /v1/payment-initiations": {
      "requests_total": 45210,
      "latency_ms_p50": 12.4,
      "latency_ms_p95": 28.1,
      "latency_ms_p99": 45.6
    }
  }
}
```

For `seed-profile`, replace endpoint latencies with `seed_duration_seconds` and `storage_bytes`.

---

## Fair comparison rules

When comparing PostgreSQL and MongoDB:

1. **Same hardware** — one machine or two VMs with matched CPU, memory, and SSD class.
2. **Same API build** — identical application commit; only `DATABASE` (and connection URL) changes.
3. **Same suite parameters** — duration, concurrency, amounts, account counts.
4. **Cold vs warm** — document whether the database was empty, seed-profiled, or reused from a prior run.
5. **Default tuning** — document connection pool size and any non-default database settings. Avoid adapter-specific optimizations that are not applied to both sides unless explicitly noted.

---

## Pass criteria

| Check | Required for completion |
|-------|-------------------------|
| SC-001–SC-015 green on adapter | Yes (before benchmarking) |
| `write-heavy`, `read-heavy`, `mixed` results recorded | Yes, per adapter |
| `seed-profile` completed and storage documented | Yes, per adapter |
| Latency SLO or regression gate | No (informational only in v1) |

A training submission should include a short comparison note: which adapter was faster on writes, reads, and seed profile, with the JSON artifacts as evidence.

---

## Reporting template

Use this structure when presenting results (demo or write-up):

1. Environment (hardware, DB versions, concurrency, duration)
2. Correctness confirmation (scenario suite pass)
3. Write-heavy TPS and p95 latency (both adapters)
4. Read-heavy p95 by endpoint (both adapters)
5. Seed-profile duration and storage size (both adapters)
6. One observed trade-off (schema, index, or query pattern) tied to the numbers

---

## Out of scope (v1)

- Direct database or repository driver benchmarks as the primary comparison method
- Multi-region or replicated cluster benchmarks
- Sustained 24-hour soak tests
- Payment rejection paths as load (AM04, DU04 under load)
- XML payload format (JSON only)
- Horizontal scaling (multiple API instances)

These may be added in a later revision.
