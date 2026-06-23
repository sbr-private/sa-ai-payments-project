# SC-015: Self-Transfer (AG01)

**Operation:** OP-003  
**Type:** Validation  
**ISO:** pain.002 `RJCT` / `AG01`

## Given

- `acc_a` with `bal.value = "100.00"`, `ccy = USD`, `status = active`

## When

`POST /payment-initiations` with `dbtrAcct` and `cdtrAcct` both referencing `<acc_a>`, `instdAmt = { "value": "10.00", "ccy": "USD" }`

## Then

- `txSts` is `RJCT`, `rsn.cd` is `AG01`
- `bal.value` remains `"100.00"`
- No `ntry` records created
