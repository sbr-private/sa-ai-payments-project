# SC-010: Concurrent Transfers

**Operation:** OP-003  
**Type:** Concurrency

## Given

- `acc_a` with `bal.value = "100.00"`, `acc_b` with `bal.value = "0.00"`, both USD

## When

Two initiations submitted **concurrently**:

| endToEndId | instdAmt |
|------------|----------|
| `E2E-SC010-A` | `{ "value": "80.00", "ccy": "USD" }` |
| `E2E-SC010-B` | `{ "value": "80.00", "ccy": "USD" }` |

Both from `acc_a` to `acc_b`.

## Then

- Exactly one `txSts = ACSC`, one `txSts = RJCT` with `rsn.cd = AM04`
- `acc_a` `bal.value = "20.00"`
- `acc_b` `bal.value = "80.00"`
- Exactly 2 `ntry` records total (one DBIT, one CRDT)

## Variant: concurrent same EndToEndId

Same `endToEndId` and body sent concurrently:

- Single transaction created
- Both responses reference same `orgnlEndToEndId`
- Balances reflect single settlement only
