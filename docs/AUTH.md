# Demo Login (demo theatre only)

Auth exists so demos feel real — a payer logs in to send money, a support agent logs in to investigate. **Not production IAM.** Keep the implementation as dumb as possible.

---

## Storage

**Demo users are not stored in the ledger database** (no `users` table in PostgreSQL, no `users` collection in MongoDB). They are hardcoded application config:

| What | Where |
|------|-------|
| Canonical user list | [fixtures/seed-users.json](./fixtures/seed-users.json) |
| Resolved by | API auth middleware (`POST /auth/login`, `X-Demo-User` header) |
| Ledger link | `accountIds` in the fixture references `accounts.id` — not a DB foreign key |

Adapter schemas document this explicitly: [mongodb/SCHEMA.md](./adapters/mongodb/SCHEMA.md#application-users-auth), [postgres/SCHEMA.md](./adapters/postgres/SCHEMA.md#application-users-auth).

---

## Demo users (UI)

Hardcoded in seed data. Password for all: `demo`

| Email | Role | UI |
|-------|------|-----|
| `payer@demo` | `payer` | Payer portal — Acme Corp account |
| `support@demo` | `support` | Control centre — sees everything |

Fixture: [fixtures/seed-users.json](./fixtures/seed-users.json)

## Benchmark service identity (CLI only)

| Email | Role | Used by |
|-------|------|---------|
| `benchmark@demo` | `benchmark` | `apps/benchmarks/` CLI only — never the UI |

Same password (`demo`). The benchmark harness sets `X-Demo-User: benchmark@demo` on every request. **No login call in the load loop** — the header is enough.

`benchmark` role has unrestricted API access (read/write any account) so load generators are not blocked by payer scope rules.

---

## API (minimal)

**`POST /auth/login`**

```json
{ "email": "payer@demo", "password": "demo" }
```

→

```json
{
  "user": {
    "email": "payer@demo",
    "displayName": "Demo Payer",
    "role": "payer",
    "accountIds": ["a1b2c3d4-e5f6-7890-abcd-ef1234567890"]
  }
}
```

Send `X-Demo-User: payer@demo` (or `support@demo`) on subsequent requests. That's it — no JWT, no bcrypt, no token expiry. The server looks up the header against the hardcoded user list.

`/health` and `/auth/login` are the only unauthenticated routes.

---

## What each role can do

| Action | `payer` | `support` | `benchmark` |
|--------|---------|-----------|-------------|
| View account / statement | Own `accountIds` only | Any | Any |
| Send payment | From own account only | No | Any (load generation) |
| Look up payment by `endToEndId` | Own payments only | Any | Any |

Support uses the **same endpoints** as payers (`GET /accounts/{id}`, `GET /payment-initiations/transactions/{endToEndId}`) — no separate search API.

---

## UI

- **Payer portal** — login page → pick/enter payer credentials → send payments
- **Control centre** — login page → enter support credentials → search by `endToEndId`

Wrong role for the app? Show a friendly redirect message. Wrong password? "Invalid credentials."

---

## Out of scope (deliberately)

- JWT, OAuth, SSO, MFA, password reset
- `/auth/me`, `/auth/logout`, `/support/*` routes
- Auth scenarios in the compliance test suite
- Benchmark auth in the compliance suite (harness uses `benchmark@demo` directly)

Implementation detail, not spec: how you store the session client-side (header, cookie, React context) is up to the app.

---

## Related documents

| Document | Role |
|----------|------|
| [PURPOSE.md](./PURPOSE.md) | Why demo login exists |
| [SEED.md](./SEED.md) | Demo users + ledger seed (separate concerns) |
| [fixtures/seed-users.json](./fixtures/seed-users.json) | Canonical user list |
| [SPEC.md](./SPEC.md) §4.3 | API auth summary |
| [adapters/mongodb/SCHEMA.md](./adapters/mongodb/SCHEMA.md#application-users-auth) | Users not in MongoDB |
| [adapters/postgres/SCHEMA.md](./adapters/postgres/SCHEMA.md#application-users-auth) | Users not in PostgreSQL |
| [scenarios/README.md](./scenarios/README.md) | Compliance tests use `benchmark@demo` |
