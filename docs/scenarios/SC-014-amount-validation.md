# SC-014: Invalid Amount (AM12)

**Operation:** OP-003  
**Type:** Validation  
**ISO:** pain.002 `RJCT` / `AM12`

## Given

- `acc_a` with `bal.value = "100.00"`, `acc_b` with `bal.value = "0.00"`, USD

## When / Then

### Zero amount

`instdAmt = { "value": "0.00", "ccy": "USD" }` → `RJCT` / `AM12`, balances unchanged

### Negative amount

`instdAmt = { "value": "-10.00", "ccy": "USD" }` → `RJCT` / `AM12`

### Excess decimal places (USD)

`instdAmt = { "value": "10.001", "ccy": "USD" }` → `RJCT` / `AM12`

### Missing endToEndId

Omit `pmtId.endToEndId` → HTTP `400` `VALIDATION_ERROR`
