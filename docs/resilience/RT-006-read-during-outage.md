# RT-006: Read Paths During Database Outage

**Operation:** OP-002, OP-004, OP-005  
**Type:** Read availability  
**Required:** Yes

## Given

- Existing `acc` with known balance and at least one settled transfer (`endToEndId` known)
- API running; `GET /ready` returns `200`

## When

1. Stop the database (same methods as RT-002).
2. Concurrently attempt:
   - `GET /v1/accounts/{acc_id}`
   - `GET /v1/payment-initiations/transactions/{endToEndId}`
   - `GET /v1/accounts/{acc_id}/statements`
3. Restore the database; wait for `GET /ready` → `200`.
4. Repeat the same three GET requests.

## Then (during outage)

- Responses are **HTTP 503** or **5xx** — not `200` with stale or fabricated data.
- Alternatively, connection failure / timeout is acceptable if the API cannot reach the database.
- Must **not** return `200` with a balance that does not match the last committed state before the outage.

## Then (after recovery)

- All three GET requests return `200` with data consistent with pre-outage state plus any transfers that completed before the outage.

## Rationale

Support and payer UIs depend on read paths during partial failures. Returning cached wrong balances is worse than an explicit error.
