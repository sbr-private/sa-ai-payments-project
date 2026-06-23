# SC-013: Account Not Found

**Operation:** OP-002, OP-003, OP-005  
**Type:** Error path  
**ISO:** `BE01` on initiation; HTTP 404 on reads

## Given

- No account `00000000-0000-0000-0000-000000000000` exists

## When / Then

### Get account

`GET /accounts/00000000-0000-0000-0000-000000000000` → HTTP `404`

### Initiation with missing debtor

pain.001 referencing missing `dbtrAcct` → `txSts = RJCT`, `rsn.cd = BE01`

### Statement for missing account

`GET /accounts/00000000-0000-0000-0000-000000000000/statements` → HTTP `404`
