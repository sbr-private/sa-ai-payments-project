# SC-003: Insufficient Funds (AM04)

**Operation:** OP-003  
**Type:** Rejection path  
**ISO:** pain.002 `RJCT` / `AM04`

## Given

- Debtor account `acc_a` with `bal.value = "10.00"`, `ccy = USD`
- Creditor account `acc_b` with `bal.value = "0.00"`, `ccy = USD`

## When

`POST /payment-initiations` with `instdAmt = { "value": "50.00", "ccy": "USD" }` and `endToEndId = E2E-SC003-0001`

## Then

- HTTP status is `201 Created` (pain.002 is the business response)
- `txInfAndSts[0].txSts` is `RJCT`
- `txInfAndSts[0].stsRsnInf[0].rsn.cd` is `AM04`
- `GET /accounts/<acc_a>` → `bal.value = "10.00"` (unchanged)
- `GET /accounts/<acc_b>` → `bal.value = "0.00"` (unchanged)
- No new `ntry` records on either account

Shape reference: [pain002-status-rjct.json](../fixtures/pain002-status-rjct.json)
