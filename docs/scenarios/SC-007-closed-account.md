# SC-007: Closed Account (AC04)

**Operation:** OP-003  
**Type:** Rejection path  
**ISO:** pain.002 `RJCT` / `AC04`

## Given

- Account `acc_closed` with `status = closed`, `bal.value = "100.00"`, `ccy = USD`
- Account `acc_active` with `status = active`, `bal.value = "0.00"`, `ccy = USD`

> Closing accounts uses a test helper (no public API in v1).

## When

Initiation from `acc_closed` → `acc_active`, `instdAmt = { "value": "10.00", "ccy": "USD" }`

## Then

- `txSts` is `RJCT`, `rsn.cd` is `AC04`
- Balances unchanged

## Also verify (creditor closed)

Initiation from `acc_active` → `acc_closed`:

- `txSts` is `RJCT`, `rsn.cd` is `AC04`
