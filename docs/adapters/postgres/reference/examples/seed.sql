-- Demo seed data — see docs/SEED.md
-- Requires schema.sql applied first.
--
-- Narrative:
--   1. Opening funding: settlement → Acme $1,000.00 (E2E-SEED-0001)
--   2. Success: Acme → Supplier $50.00 (E2E-INV-2024-0558)
--   3. Failed:  Acme → Supplier $2,000.00 (E2E-INV-2024-0999, AM04)
--
-- Apply: psql "$DATABASE_URL" -f docs/adapters/postgres/reference/examples/seed.sql

BEGIN;

TRUNCATE statement_entries, payment_transactions, accounts CASCADE;

-- ---------------------------------------------------------------------------
-- accounts
-- ---------------------------------------------------------------------------

INSERT INTO accounts (
    id, owner_nm, owner_external_id, ccy, bal_value_minor, status, cre_dt_tm, schema_version
) VALUES
    (
        'c0000000-0000-0000-0000-000000000001',
        'Payments Co Settlement',
        NULL,
        'USD',
        990000,
        'active',
        '2026-06-23T11:00:00Z',
        1
    ),
    (
        'a1b2c3d4-e5f6-7890-abcd-ef1234567890',
        'Acme Corp',
        'user_123',
        'USD',
        95000,
        'active',
        '2026-06-23T11:00:00Z',
        1
    ),
    (
        'b2c3d4e5-f6a7-8901-bcde-f12345678901',
        'Supplier Ltd',
        NULL,
        'USD',
        5000,
        'active',
        '2026-06-23T11:00:00Z',
        1
    );

-- ---------------------------------------------------------------------------
-- payment_transactions
-- ---------------------------------------------------------------------------

INSERT INTO payment_transactions (
    tx_id,
    end_to_end_id,
    instr_id,
    dbtr_acct_id,
    cdtr_acct_id,
    instd_amt_value_minor,
    instd_amt_ccy,
    tx_sts,
    sts_rsn_inf,
    rmt_inf,
    idempotency_dbtr_acct_id,
    idempotency_cdtr_acct_id,
    idempotency_amt_value_minor,
    idempotency_amt_ccy,
    cre_dt_tm,
    schema_version
) VALUES
    (
        'd1e2f3a4-b5c6-7890-abcd-ef1234567890',
        'E2E-SEED-0001',
        'INSTR-SEED-0001',
        'c0000000-0000-0000-0000-000000000001',
        'a1b2c3d4-e5f6-7890-abcd-ef1234567890',
        100000,
        'USD',
        'ACSC',
        NULL,
        '{"ustrd": ["Opening balance funding"]}',
        'c0000000-0000-0000-0000-000000000001',
        'a1b2c3d4-e5f6-7890-abcd-ef1234567890',
        100000,
        'USD',
        '2026-06-23T11:30:00Z',
        1
    ),
    (
        'f47ac10b-58cc-4372-a567-0e02b2c3d479',
        'E2E-INV-2024-0558',
        'INSTR-20260623-0001',
        'a1b2c3d4-e5f6-7890-abcd-ef1234567890',
        'b2c3d4e5-f6a7-8901-bcde-f12345678901',
        5000,
        'USD',
        'ACSC',
        NULL,
        '{"ustrd": ["Invoice 2024-0558"]}',
        'a1b2c3d4-e5f6-7890-abcd-ef1234567890',
        'b2c3d4e5-f6a7-8901-bcde-f12345678901',
        5000,
        'USD',
        '2026-06-23T12:00:01Z',
        1
    ),
    (
        'a9b8c7d6-e5f4-3210-abcd-ef9876543210',
        'E2E-INV-2024-0999',
        'INSTR-20260623-0002',
        'a1b2c3d4-e5f6-7890-abcd-ef1234567890',
        'b2c3d4e5-f6a7-8901-bcde-f12345678901',
        200000,
        'USD',
        'RJCT',
        '[{"rsn": {"cd": "AM04"}, "addtlInf": ["Insufficient funds on debtor account"]}]',
        '{"ustrd": ["Invoice 2024-0999"]}',
        'a1b2c3d4-e5f6-7890-abcd-ef1234567890',
        'b2c3d4e5-f6a7-8901-bcde-f12345678901',
        200000,
        'USD',
        '2026-06-23T12:15:00Z',
        1
    );

-- ---------------------------------------------------------------------------
-- statement_entries (ACSC transactions only — no entries for RJCT)
-- ---------------------------------------------------------------------------

INSERT INTO statement_entries (
    ntry_ref,
    acct_id,
    tx_id,
    end_to_end_id,
    amt_value_minor,
    amt_ccy,
    cdt_dbt_ind,
    bal_value_minor,
    bal_ccy,
    bal_cdt_dbt_ind,
    bookg_dt,
    sts,
    cre_dt_tm,
    schema_version
) VALUES
    (
        'e000a001-0000-4000-8000-000000000001',
        'c0000000-0000-0000-0000-000000000001',
        'd1e2f3a4-b5c6-7890-abcd-ef1234567890',
        'E2E-SEED-0001',
        100000,
        'USD',
        'DBIT',
        990000,
        'USD',
        'CRDT',
        '2026-06-23',
        'BOOK',
        '2026-06-23T11:30:00.100Z',
        1
    ),
    (
        'e000a002-0000-4000-8000-000000000002',
        'a1b2c3d4-e5f6-7890-abcd-ef1234567890',
        'd1e2f3a4-b5c6-7890-abcd-ef1234567890',
        'E2E-SEED-0001',
        100000,
        'USD',
        'CRDT',
        100000,
        'USD',
        'CRDT',
        '2026-06-23',
        'BOOK',
        '2026-06-23T11:30:00.100Z',
        1
    ),
    (
        'e001a2b3-c4d5-6789-abcd-ef0123456789',
        'a1b2c3d4-e5f6-7890-abcd-ef1234567890',
        'f47ac10b-58cc-4372-a567-0e02b2c3d479',
        'E2E-INV-2024-0558',
        5000,
        'USD',
        'DBIT',
        95000,
        'USD',
        'CRDT',
        '2026-06-23',
        'BOOK',
        '2026-06-23T12:00:01.100Z',
        1
    ),
    (
        'e002b3c4-d5e6-7890-bcde-f12345678901',
        'b2c3d4e5-f6a7-8901-bcde-f12345678901',
        'f47ac10b-58cc-4372-a567-0e02b2c3d479',
        'E2E-INV-2024-0558',
        5000,
        'USD',
        'CRDT',
        5000,
        'USD',
        'CRDT',
        '2026-06-23',
        'BOOK',
        '2026-06-23T12:00:01.100Z',
        1
    );

COMMIT;
