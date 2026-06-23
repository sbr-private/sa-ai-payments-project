# SC-011: Statement Pagination

**Operation:** OP-005  
**Type:** Read path  
**ISO:** camt.053

## Given

- Account `acc_a` with 5 booked `ntry` records (from 5 settled transactions)

## When

1. `GET /accounts/<acc_a>/statements?limit=2`
2. `GET /accounts/<acc_a>/statements?limit=2&cursor=<nextCursor>`
3. `GET /accounts/<acc_a>/statements?limit=2&cursor=<nextCursor>`

## Then

**Step 1:** 2 entries, `hasMore = true`, `nextCursor` non-null, newest first  
**Step 2:** next 2 entries, no overlap  
**Step 3:** remaining 1 entry, `hasMore = false`, `nextCursor = null`

Shape per [camt053-statement.json](../fixtures/camt053-statement.json).

## Edge case

`limit=0` or `limit=101` → HTTP `400` `VALIDATION_ERROR`
