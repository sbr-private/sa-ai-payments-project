# SC-002: Credit Transfer Initiation

**Operation:** OP-003  
**Type:** Happy path  
**ISO:** pain.001 → pain.002

## Given

- Debtor account `acc_a` with `bal.value = "1000.00"`, `ccy = USD`, `status = active`
- Creditor account `acc_b` with `bal.value = "0.00"`, `ccy = USD`, `status = active`

> Setup note: seed balances via test helper (simulates prior camt.053 credits).

## When

`POST /payment-initiations` with body based on [pain001-initiation.json](../fixtures/pain001-initiation.json):

- `dbtrAcct.id.othr.id` = `<acc_a>`
- `cdtrAcct.id.othr.id` = `<acc_b>`
- `instdAmt` = `{ "value": "50.00", "ccy": "USD" }`
- `endToEndId` = `E2E-SC002-0001`

## Then

- HTTP status is `201 Created`
- Response is pain.002 per [pain002-status-acsc.json](../fixtures/pain002-status-acsc.json)
- `txInfAndSts[0].txSts` is `ACSC`
- `orgnlGrpInfAndSts.grpSts` is `ACSC`
- `GET /accounts/<acc_a>` → `bal.value = "950.00"`
- `GET /accounts/<acc_b>` → `bal.value = "50.00"`
- `GET /accounts/<acc_a>/statements` includes a `DBIT` entry with `amt.value = "50.00"`
- `GET /accounts/<acc_b>/statements` includes a `CRDT` entry with `amt.value = "50.00"`
