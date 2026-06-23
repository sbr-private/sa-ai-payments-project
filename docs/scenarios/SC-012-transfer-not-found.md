# SC-012: Transaction Not Found

**Operation:** OP-004  
**Type:** Error path

## Given

- No transaction with `endToEndId = E2E-MISSING-0001` exists

## When

`GET /payment-initiations/transactions/E2E-MISSING-0001`

## Then

- HTTP status is `404 Not Found`
- Error envelope per [error-not-found.json](../fixtures/error-not-found.json)
