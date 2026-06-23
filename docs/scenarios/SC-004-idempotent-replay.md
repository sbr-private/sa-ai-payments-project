# SC-004: EndToEndId Replay

**Operation:** OP-003  
**Type:** Idempotency  
**ISO:** `PmtId.EndToEndId`

## Given

- Debtor `acc_a` with `bal.value = "1000.00"`, creditor `acc_b` with `bal.value = "0.00"`
- Prior successful initiation with `endToEndId = E2E-SC004-0001`, `instdAmt = { "value": "30.00", "ccy": "USD" }`

## When

Identical pain.001 submitted again with same `endToEndId` and equivalent `cdtTrfTxInf` body.

## Then

- HTTP status is `200 OK`
- `txInfAndSts[0].txSts` is `ACSC` (original status)
- `GET /accounts/<acc_a>` → `bal.value = "970.00"` (not further debited)
- `GET /accounts/<acc_b>` → `bal.value = "30.00"` (not further credited)
- Total `ntry` count across both accounts: exactly 2
