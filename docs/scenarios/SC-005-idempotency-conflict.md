# SC-005: EndToEndId Conflict (DU04)

**Operation:** OP-003  
**Type:** Idempotency error  
**ISO:** pain.002 `RJCT` / `DU04`

## Given

- Accounts `acc_a`, `acc_b`, `acc_c` as in SC-002
- Prior successful initiation: `endToEndId = E2E-SC005-0001`, `acc_a` → `acc_b`, `instdAmt = { "value": "20.00", "ccy": "USD" }`

## When

New initiation with same `endToEndId` but `cdtrAcct` = `acc_c` (different creditor).

## Then

- HTTP status is `409 Conflict`
- `txInfAndSts[0].txSts` is `RJCT`
- `txInfAndSts[0].stsRsnInf[0].rsn.cd` is `DU04`
- Balances unchanged from post-original-transfer state
