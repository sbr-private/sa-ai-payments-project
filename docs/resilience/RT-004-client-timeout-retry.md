# RT-004: Client Timeout and Idempotent Retry

**Operation:** OP-003  
**Type:** Client behaviour under uncertainty  
**Required:** Yes

## Given

- `acc_debtor` with `bal.value = "200.00"`, `acc_creditor` with `bal.value = "0.00"`, both USD
- Harness can set a **short client-side HTTP timeout** (e.g. 500 ms)

## When

1. Introduce database latency (slow query hook, `tc netem`, or overloaded single-threaded test mode) so the initiation may exceed the client timeout.
2. Client submits `POST /v1/payment-initiations` with `endToEndId = E2E-RT004`, `instdAmt = { "value": "30.00", "ccy": "USD" }`.
3. Client receives timeout or connection error (or ambiguous 5xx).
4. After API and database are healthy, client **retries the identical request** (same body, same `endToEndId`).
5. Query status and balances.

## Then

- Exactly **one** settlement: debtor `170.00`, creditor `30.00`.
- Second response is HTTP `200` replay or `201` — either acceptable if `txSts = ACSC` and balances reflect a single transfer.
- No `DU04` on identical retry body.
- If the first attempt actually settled before the timeout, the retry returns the original pain.002 (SC-004).

## Relation to SC-004

This test extends [SC-004](../scenarios/SC-004-idempotent-replay.md) with **network/process uncertainty** between attempts. SC-004 must pass before RT-004 is meaningful.
