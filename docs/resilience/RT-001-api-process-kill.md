# RT-001: API Process Killed During Transfer

**Operation:** OP-003  
**Type:** Process failure  
**Required:** Yes

## Given

- `acc_debtor` with `bal.value = "1000.00"`, `acc_creditor` with `bal.value = "0.00"`, both USD
- API running against the adapter under test
- Baseline balances recorded

## When

1. Harness begins `POST /v1/payment-initiations` with `endToEndId = E2E-RT001`, `instdAmt = { "value": "50.00", "ccy": "USD" }`.
2. While the request is in flight (harness detects open connection or uses a deliberate slow path), **kill the API process** (`SIGKILL` — not graceful shutdown).
3. Restart the API against the **same** database (no data wipe).
4. Query `GET /v1/payment-initiations/transactions/E2E-RT001`.

## Then

Exactly one of the following — document which occurred:

| Outcome | Expected state |
|---------|----------------|
| **A — Settled before kill** | `txSts = ACSC`; debtor `950.00`, creditor `50.00`; two `ntry` records |
| **B — Not settled** | `404` on status lookup **or** no `ACSC` record; balances unchanged at `1000.00` / `0.00` |

**Never:**

- Partial debit without credit (debtor reduced, creditor unchanged)
- `ACSC` without matching double-entry `ntry` pair
- Duplicate settlement after a subsequent retry with the same `endToEndId` and body (HTTP 200 replay, single settlement)

## Notes

Outcome A vs B depends on timing. Both are valid if invariants hold. The test passes on **consistency**, not on which branch occurred.
