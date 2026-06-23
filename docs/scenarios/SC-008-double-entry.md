# SC-008: Double-Entry Invariant

**Operation:** OP-003  
**Type:** Invariant  
**ISO:** camt.053 `Ntry`

## Given

- `acc_a` with `bal.value = "500.00"`, `acc_b` with `bal.value = "100.00"`, both USD

## When

Initiation `acc_a` → `acc_b`, `instdAmt = { "value": "75.00", "ccy": "USD" }`, `endToEndId = E2E-SC008-0001`

## Then

- `txSts` is `ACSC`
- Exactly 2 statement entries for this `endToEndId`:
  - `acc_a`: `cdtDbtInd = DBIT`, `amt.value = "75.00"`
  - `acc_b`: `cdtDbtInd = CRDT`, `amt.value = "75.00"`
- Magnitudes equal; one debit and one credit
