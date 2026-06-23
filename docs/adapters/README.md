# Database Adapters

Physical schemas for the two initial backends. Both implement the same [LedgerRepository](../SPEC.md#10-db-portability-rules) contract against identical API behaviour.

| Backend | Schema | DDL | Demo seed |
|---------|--------|-----|-----------|
| **MongoDB** | [mongodb/SCHEMA.md](./mongodb/SCHEMA.md) | [reference/create-collections.js](./mongodb/reference/create-collections.js) | [seed.json](./mongodb/reference/examples/seed.json) · [seed.js](./mongodb/reference/examples/seed.js) |
| **PostgreSQL** | [postgres/SCHEMA.md](./postgres/SCHEMA.md) | [reference/schema.sql](./postgres/reference/schema.sql) | [seed.sql](./postgres/reference/examples/seed.sql) |

## Shared logical model

| Entity | MongoDB collection | PostgreSQL table |
|--------|-------------------|------------------|
| Account | `accounts` | `accounts` |
| PaymentTransaction | `payment_transactions` | `payment_transactions` |
| StatementEntry | `statement_entries` | `statement_entries` |

Amounts are stored as **minor-unit integers** in both adapters. The API uses ISO decimal strings — see [iso20022/MAPPING.md](../iso20022/MAPPING.md).

## Demo users

Demo login users (`payer@demo`, etc.) are **not** stored in either database. See [AUTH.md](../AUTH.md) and [fixtures/seed-users.json](../fixtures/seed-users.json).

## Demo seed

Narrative and UUIDs: [SEED.md](../SEED.md)

```bash
# PostgreSQL
psql "$DATABASE_URL" \
  -f docs/adapters/postgres/reference/schema.sql \
  -f docs/adapters/postgres/reference/examples/seed.sql

# MongoDB
mongosh "$MONGODB_URI" --file docs/adapters/mongodb/reference/create-collections.js
mongosh "$MONGODB_URI" --file docs/adapters/mongodb/reference/examples/seed.js
```

## Compliance testing

Adapter correctness is proven by [scenarios/](../scenarios/) SC-001–015, not by demo seed data.
