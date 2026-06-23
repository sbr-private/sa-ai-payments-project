# AmazingPayments — Frontend

Node.js / Express demo UI for the ISO 20022 payments ledger. Styled with [MongoDB Leafy Green](https://www.mongodb.design/foundations/palette) light-mode palette.

Two portals share the same Spring Boot API:

| Portal | Route | Demo user |
|--------|-------|-----------|
| **Payer** | `/payer` | `payer@demo` / `demo` |
| **Control centre** | `/support` | `support@demo` / `demo` |

Spec: [docs/SPEC.md](../../docs/SPEC.md) · Auth: [docs/AUTH.md](../../docs/AUTH.md) · Seed: [docs/SEED.md](../../docs/SEED.md)

## Prerequisites

- Node.js 20+
- Ledger backend running at `http://localhost:8080/v1` (see [../backend/README.md](../backend/README.md))

For UI-only development without the backend, enable fixture mode (see below).

## Install and run

```bash
cd implementation/frontend
cp .env.example .env
npm install
npm start
```

Open [http://localhost:3000](http://localhost:3000).

Dev mode with auto-reload:

```bash
npm run dev
```

## API documentation

All APIs are documented with OpenAPI 3.1 and served via Swagger UI:

| Docs | URL | Source |
|------|-----|--------|
| **Ledger API** | [/api-docs/ledger](http://localhost:3000/api-docs/ledger) | [docs/openapi.yaml](../../docs/openapi.yaml) |
| **Frontend UI API** | [/api-docs/ui](http://localhost:3000/api-docs/ui) | [openapi/frontend.yaml](./openapi/frontend.yaml) |

The browser also proxies authenticated ledger calls at `/api/*`, injecting `X-Demo-User` from the session.

## Configuration

| Variable | Default | Purpose |
|----------|---------|---------|
| `PORT` | `3000` | Frontend HTTP port |
| `SESSION_SECRET` | — | Express session signing secret |
| `API_URL` | `http://localhost:8080/v1` | Ledger backend base URL |
| `USE_FIXTURES` | `false` | Serve golden fixtures instead of calling the backend |

### Fixture mode

When backend endpoints are still being built, run with fixtures:

```bash
USE_FIXTURES=true npm start
```

This uses [docs/fixtures/](../../docs/fixtures/) for accounts, statements, payments, and login.

## Demo flows

### Payer — send a payment

1. Sign in as `payer@demo`
2. View Acme Corp balance on the dashboard
3. **New payment** → submit pain.001 with a fresh `endToEndId`
4. On **ACSC**, you are redirected to the dashboard with refreshed balance and statement
5. **Payment status** — look up any of your payments by `endToEndId` (or click a statement row)

### Support — investigate a rejection

1. Sign in as `support@demo`
2. Search `E2E-INV-2024-0999` (requires ledger `GET /payment-initiations/transactions/{endToEndId}` or `USE_FIXTURES=true`)
3. Expect `txSts: RJCT`, reason `AM04`
4. **Drill into debtor account** — compare balance vs instructed amount
5. Use **Load more** on statements when paginated

## Architecture

```
Browser
  → Express (session, EJS pages)
    → ledgerClient /api proxy
      → Spring Boot /v1 (X-Demo-User header)
```

- **Session auth** — `POST /login` calls ledger `POST /auth/login`, stores user in session
- **Role gates** — payers cannot access `/support`; support cannot access `/payer`
- **ISO visibility** — UI surfaces `endToEndId`, `txSts`, `stsRsnInf` per spec

## Package layout

```
src/
  server.js           Express entrypoint
  config.js           Environment config
  lib/
    ledgerClient.js   Typed fetch wrapper to ledger API
    fixtures.js       Fixture responses (USE_FIXTURES)
  middleware/
    auth.js           Session guards and role checks
  routes/
    auth.js           Login / logout
    payer.js          Payer portal pages
    support.js        Control centre pages
    apiProxy.js       /api/* reverse proxy
    docs.js           Swagger UI
  views/              EJS templates
  public/css/         Styles
openapi/
  frontend.yaml       UI route documentation
```
