# SC-009: Balance Integrity

**Operation:** OP-003 (sequential)  
**Type:** Invariant

## Given

- `acc_a` and `acc_b` with `bal.value = "0.00"`, USD

## When

1. Seed credit `acc_a` with `"500.00"` (test helper, `endToEndId = E2E-SEED-0001`)
2. Initiate `acc_a` → `acc_b`, `"150.00"`, `E2E-SC009-0001`
3. Initiate `acc_b` → `acc_a`, `"50.00"`, `E2E-SC009-0002`
4. Initiate `acc_a` → `acc_b`, `"80.00"`, `E2E-SC009-0003`

## Then

For each account:

- `GET /accounts/<id>` `bal.value` equals net of all `ntry` amounts (debits subtract, credits add)
- Closing balance `stmt.bal[CLBD]` matches `bal`

Expected:

- `acc_a`: `"320.00"` (500 − 150 + 50 − 80)
- `acc_b`: `"180.00"` (150 − 50 + 80)
