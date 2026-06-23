# RT-002: Database Unavailable During Transfer

**Operation:** OP-003  
**Type:** Infrastructure failure  
**Required:** Yes

## Given

- `acc_debtor` with `bal.value = "500.00"`, `acc_creditor` with `bal.value = "0.00"`, both USD
- API running and `GET /ready` returns `200`

## When

1. Make the database unreachable from the API (stop container, drop network route, or reject connections).
2. Submit `POST /v1/payment-initiations` with `endToEndId = E2E-RT002`, `instdAmt = { "value": "25.00", "ccy": "USD" }`.
3. Restore database connectivity.
4. Wait until `GET /ready` returns `200`.
5. Query transaction status and account balances.

## Then

- Initiation request returns **HTTP 5xx** or the client connection fails (timeout/reset) — not `201` with `ACSC`.
- After recovery: balances are `500.00` / `0.00` **or** a single complete `ACSC` settlement — same rules as RT-001 outcome A/B.
- No partial settlement.
- Retry with the same `endToEndId` and body: idempotent result (SC-004 semantics); no double debit.

## Then (readiness)

- `GET /ready` returns `503` (or equivalent) while the database is down, if the endpoint is implemented.
