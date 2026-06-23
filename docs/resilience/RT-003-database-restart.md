# RT-003: Database Restart and Recovery

**Operation:** OP-003, OP-002  
**Type:** Infrastructure recovery  
**Required:** Yes

## Given

- Funded accounts ready for transfers
- API running; at least one successful `ACSC` transfer already recorded (setup via API)
- Baseline balances recorded

## When

1. **Stop** the database process cleanly (`docker stop`, `pg_ctl stop`, etc.).
2. Confirm API reports degraded state (`GET /ready` → `503`, or write requests fail with 5xx).
3. **Start** the database again without wiping data volumes.
4. Wait for API to accept traffic (`GET /ready` → `200`; implementer may require API restart — document if so).
5. Submit a new transfer: `endToEndId = E2E-RT003`, `instdAmt = { "value": "10.00", "ccy": "USD" }`.
6. `GET` prior accounts and statement entries from before the restart.

## Then

- Pre-restart `ACSC` transfers remain visible with correct `txSts` and balances.
- New transfer after recovery returns `ACSC` with correct balance updates.
- Statement entries from before the restart are still present (append-only ledger).
- No duplicate or missing `ntry` records for pre-restart transactions.

## Record

Document whether the API recovered automatically after database restart or required a manual API restart. Automatic recovery is preferred but not mandatory for v1; behaviour must be documented per adapter.
