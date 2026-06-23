# SC-001: Register Account

**Operation:** OP-001  
**Type:** Happy path

## Given

- No prior state required

## When

`POST /accounts` with body per [create-account-request.json](../fixtures/create-account-request.json):

```json
{
  "owner": { "nm": "Acme Corp", "id": { "othr": { "id": "user_123" } } },
  "ccy": "USD"
}
```

## Then

- HTTP status is `201 Created`
- Response matches shape of [create-account-response.json](../fixtures/create-account-response.json)
- `bal.value` is `"0.00"` and `bal.ccy` is `USD`
- `status` is `active`
- `id` is a valid UUID
- `creDtTm` is valid ISO-8601 UTC
