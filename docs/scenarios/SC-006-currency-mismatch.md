# SC-006: Currency Mismatch (CURR)

**Operation:** OP-003  
**Type:** Rejection path  
**ISO:** pain.002 `RJCT` / `CURR`

## Given

- Account `acc_usd` with `ccy = USD`, `bal.value = "500.00"`
- Account `acc_eur` with `ccy = EUR`, `bal.value = "0.00"`

## When

`POST /payment-initiations` from `acc_usd` to `acc_eur`, `instdAmt = { "value": "10.00", "ccy": "USD" }`

## Then

- `txSts` is `RJCT`, `rsn.cd` is `CURR`
- Both balances unchanged
- No `ntry` records created
