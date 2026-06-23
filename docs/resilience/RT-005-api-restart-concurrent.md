# RT-005: API Restart Under Concurrent Load

**Operation:** OP-003  
**Type:** Process failure + concurrency  
**Required:** Yes

## Given

- `acc_a` with `bal.value = "1000.00"`, `acc_b` and `acc_c` with `bal.value = "0.00"`, all USD
- Harness can submit concurrent initiations

## When

1. Start **10 concurrent** `POST /v1/payment-initiations` requests (unique `endToEndId` each, `$10.00` transfers from `acc_a` to `acc_b` or `acc_c`).
2. **Kill the API process** (`SIGKILL`) during the burst.
3. Restart the API.
4. For each `endToEndId`, poll `GET /v1/payment-initiations/transactions/{endToEndId}` until resolved or timeout.
5. Re-read all three account balances.

## Then

- Each `endToEndId` is either `ACSC` or absent (not `PDNG` indefinitely after recovery).
- Total debited from `acc_a` equals sum of credited amounts to `acc_b` and `acc_c`.
- `acc_a` balance + `acc_b` balance + `acc_c` balance = `1000.00` (conservation).
- Number of `ACSC` results ≤ 10; no duplicate `endToEndId` settlements.
- At most one `ACSC` per unique `endToEndId`.

## Relation to SC-010

Builds on [SC-010](../scenarios/SC-010-concurrent-transfers.md) with an API crash mid-burst. Concurrent correctness without crash is a prerequisite.
