# Demo Seed Data

Fixed ledger dataset for local demos, UI development, and manual smoke tests.

**Not** used by the compliance scenario suite ([scenarios/](scenarios/)) — SC-001–015 manage their own setup.

---

## Apply seed

Run **after** adapter schema is created:

```bash
# PostgreSQL
psql "$DATABASE_URL" \
  -f docs/adapters/postgres/reference/schema.sql \
  -f docs/adapters/postgres/reference/examples/seed.sql

# MongoDB (from project root)
mongosh "$MONGODB_URI" --file docs/adapters/mongodb/reference/examples/seed.js
```

MongoDB bootstrap order: `create-collections.js` → `seed.js` (see [mongodb/SCHEMA.md](adapters/mongodb/SCHEMA.md)).

---

## Canonical files

| Format | Path | Contents |
|--------|------|----------|
| **This doc** | [SEED.md](./SEED.md) | Narrative, UUIDs, demo hints |
| **JSON** | [adapters/mongodb/reference/examples/seed.json](adapters/mongodb/reference/examples/seed.json) | `accounts`, `payment_transactions`, `statement_entries` |
| **SQL** | [adapters/postgres/reference/examples/seed.sql](adapters/postgres/reference/examples/seed.sql) | Same data as JSON |
| **Users** (API only) | [fixtures/seed-users.json](fixtures/seed-users.json) | `payer@demo`, `support@demo`, `benchmark@demo` |

Demo users are **not** inserted by `seed.sql` / `seed.js` — they live in application config. See [AUTH.md](./AUTH.md).

The `users` array in `seed.json` is documentation-only (mirrors `seed-users.json`).

---

## Narrative

| # | Event | Amount | EndToEndId | TxSts |
|---|-------|--------|------------|-------|
| 1 | Settlement → Acme (opening) | $1,000.00 | `E2E-SEED-0001` | ACSC |
| 2 | Acme → Supplier (invoice 0558) | $50.00 | `E2E-INV-2024-0558` | ACSC |
| 3 | Acme → Supplier (invoice 0999) | $2,000.00 | `E2E-INV-2024-0999` | RJCT / AM04 |

### Resulting balances

| Account | ID | Balance |
|---------|-----|---------|
| Acme Corp | `a1b2c3d4-e5f6-7890-abcd-ef1234567890` | **$950.00** |
| Supplier Ltd | `b2c3d4e5-f6a7-8901-bcde-f12345678901` | **$50.00** |
| Payments Co Settlement *(internal)* | `c0000000-0000-0000-0000-000000000001` | $9,900.00 |

The settlement account exists for double-entry integrity. It is **not** shown in the payer UI or control centre.

---

## Demo users

Password **`demo`** for all — full detail in [AUTH.md](./AUTH.md).

| Email | Role | UI / tool |
|-------|------|-----------|
| `payer@demo` | payer | Payer portal → Acme account |
| `support@demo` | support | Control centre → all data |
| `benchmark@demo` | benchmark | Benchmark CLI only |

---

## Demo scripts (quick reference)

### Payer — send a new payment

1. Log in as `payer@demo`
2. Debtor: Acme (`a1b2c3d4-e5f6-7890-abcd-ef1234567890`) — balance **$950.00**
3. Beneficiary: Supplier (`b2c3d4e5-f6a7-8901-bcde-f12345678901`)
4. Submit pain.001 with a new `endToEndId`

### Support — investigate failed payment

1. Log in as `support@demo`
2. `GET /payment-initiations/transactions/E2E-INV-2024-0999`
3. Expect `txSts: RJCT`, `rsn.cd: AM04` — $2,000 instructed vs $950 available
4. Drill into Acme account for balance confirmation

Full scripts: [PURPOSE.md](./PURPOSE.md#demo-scripts-planned)

---

## Fixed identifiers

| Entity | UUID / ID |
|--------|-----------|
| Acme account | `a1b2c3d4-e5f6-7890-abcd-ef1234567890` |
| Supplier account | `b2c3d4e5-f6a7-8901-bcde-f12345678901` |
| Settlement account | `c0000000-0000-0000-0000-000000000001` |
| Tx opening | `d1e2f3a4-b5c6-7890-abcd-ef1234567890` |
| Tx success | `f47ac10b-58cc-4372-a567-0e02b2c3d479` |
| Tx failed | `a9b8c7d6-e5f4-3210-abcd-ef9876543210` |
| E2E opening | `E2E-SEED-0001` |
| E2E success | `E2E-INV-2024-0558` |
| E2E failed | `E2E-INV-2024-0999` |

---

## Related documents

| Document | Role |
|----------|------|
| [PURPOSE.md](./PURPOSE.md) | Why this data exists |
| [AUTH.md](./AUTH.md) | Demo login users |
| [SPEC.md](./SPEC.md) | Domain rules |
| [adapters/mongodb/SCHEMA.md](adapters/mongodb/SCHEMA.md) | MongoDB physical model |
| [adapters/postgres/SCHEMA.md](adapters/postgres/SCHEMA.md) | PostgreSQL physical model |
| [adapters/README.md](./adapters/README.md) | Adapter index |
| [scenarios/README.md](./scenarios/README.md) | Compliance tests |
