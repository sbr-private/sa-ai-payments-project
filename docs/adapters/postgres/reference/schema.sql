-- Payments Ledger — PostgreSQL schema (v1)
-- Spec: docs/SPEC.md v2.1.0
-- Apply: psql "$DATABASE_URL" -f docs/adapters/postgres/reference/schema.sql

BEGIN;

-- ---------------------------------------------------------------------------
-- Enum types
-- ---------------------------------------------------------------------------

CREATE TYPE account_status AS ENUM ('active', 'closed');

CREATE TYPE tx_status AS ENUM ('PDNG', 'ACSC', 'RJCT');

CREATE TYPE cdt_dbt_ind AS ENUM ('DBIT', 'CRDT');

CREATE TYPE entry_status AS ENUM ('BOOK');

-- ---------------------------------------------------------------------------
-- accounts
-- ---------------------------------------------------------------------------

CREATE TABLE accounts (
    id                  UUID            PRIMARY KEY,
    owner_nm            VARCHAR(140)    NOT NULL,
    owner_external_id   VARCHAR(35),
    ccy                 CHAR(3)         NOT NULL,
    bal_value_minor     BIGINT          NOT NULL,
    status              account_status  NOT NULL DEFAULT 'active',
    cre_dt_tm           TIMESTAMPTZ     NOT NULL,
    schema_version      SMALLINT        NOT NULL DEFAULT 1,

    CONSTRAINT accounts_bal_non_negative
        CHECK (bal_value_minor >= 0),

    CONSTRAINT accounts_ccy_uppercase
        CHECK (ccy ~ '^[A-Z]{3}$'),

    CONSTRAINT accounts_schema_version_v1
        CHECK (schema_version = 1)
);

COMMENT ON TABLE accounts IS 'Cash accounts with current booked balance (ISO CashAccount40 + Party)';
COMMENT ON COLUMN accounts.bal_value_minor IS 'Booked balance in ISO 4217 minor units';
COMMENT ON COLUMN accounts.owner_external_id IS 'ISO PartyIdentification135 Id.Othr.Id (optional)';

-- ---------------------------------------------------------------------------
-- payment_transactions
-- ---------------------------------------------------------------------------

CREATE TABLE payment_transactions (
    tx_id                       UUID        PRIMARY KEY,
    end_to_end_id               VARCHAR(35) NOT NULL,
    instr_id                    VARCHAR(35),
    dbtr_acct_id                UUID        NOT NULL REFERENCES accounts (id),
    cdtr_acct_id                UUID        NOT NULL REFERENCES accounts (id),
    instd_amt_value_minor       BIGINT      NOT NULL,
    instd_amt_ccy               CHAR(3)     NOT NULL,
    tx_sts                      tx_status   NOT NULL,
    sts_rsn_inf                 JSONB,
    rmt_inf                     JSONB,
    idempotency_dbtr_acct_id    UUID        NOT NULL,
    idempotency_cdtr_acct_id    UUID        NOT NULL,
    idempotency_amt_value_minor BIGINT      NOT NULL,
    idempotency_amt_ccy         CHAR(3)     NOT NULL,
    cre_dt_tm                   TIMESTAMPTZ NOT NULL,
    schema_version              SMALLINT    NOT NULL DEFAULT 1,

    CONSTRAINT payment_transactions_end_to_end_id_unique
        UNIQUE (end_to_end_id),

    CONSTRAINT payment_transactions_no_self_transfer
        CHECK (dbtr_acct_id <> cdtr_acct_id),

    CONSTRAINT payment_transactions_instd_amt_positive
        CHECK (instd_amt_value_minor > 0),

    CONSTRAINT payment_transactions_instd_amt_ccy_uppercase
        CHECK (instd_amt_ccy ~ '^[A-Z]{3}$'),

    CONSTRAINT payment_transactions_idempotency_amt_positive
        CHECK (idempotency_amt_value_minor > 0),

    CONSTRAINT payment_transactions_idempotency_ccy_uppercase
        CHECK (idempotency_amt_ccy ~ '^[A-Z]{3}$'),

    CONSTRAINT payment_transactions_rjct_requires_reason
        CHECK (tx_sts <> 'RJCT' OR sts_rsn_inf IS NOT NULL),

    CONSTRAINT payment_transactions_schema_version_v1
        CHECK (schema_version = 1)
);

COMMENT ON TABLE payment_transactions IS 'One row per CdtTrfTxInf; idempotency via end_to_end_id';
COMMENT ON COLUMN payment_transactions.end_to_end_id IS 'ISO PmtId.EndToEndId — idempotency key (max 35 chars)';
COMMENT ON COLUMN payment_transactions.sts_rsn_inf IS 'ISO StsRsnInf array; required when tx_sts = RJCT';
COMMENT ON COLUMN payment_transactions.idempotency_dbtr_acct_id IS 'Normalized fingerprint for SPEC §7.2 replay detection';

CREATE INDEX idx_payment_transactions_dbtr_cre_dt_tm
    ON payment_transactions (dbtr_acct_id, cre_dt_tm DESC);

-- ---------------------------------------------------------------------------
-- statement_entries
-- ---------------------------------------------------------------------------

CREATE TABLE statement_entries (
    ntry_ref            UUID            PRIMARY KEY,
    acct_id             UUID            NOT NULL REFERENCES accounts (id),
    tx_id               UUID            NOT NULL REFERENCES payment_transactions (tx_id),
    end_to_end_id       VARCHAR(35)     NOT NULL,
    amt_value_minor     BIGINT          NOT NULL,
    amt_ccy             CHAR(3)         NOT NULL,
    cdt_dbt_ind         cdt_dbt_ind     NOT NULL,
    bal_value_minor     BIGINT          NOT NULL,
    bal_ccy             CHAR(3)         NOT NULL,
    bal_cdt_dbt_ind     cdt_dbt_ind     NOT NULL,
    bookg_dt            DATE            NOT NULL,
    sts                 entry_status    NOT NULL DEFAULT 'BOOK',
    cre_dt_tm           TIMESTAMPTZ     NOT NULL,
    schema_version      SMALLINT        NOT NULL DEFAULT 1,

    CONSTRAINT statement_entries_amt_positive
        CHECK (amt_value_minor > 0),

    CONSTRAINT statement_entries_amt_ccy_uppercase
        CHECK (amt_ccy ~ '^[A-Z]{3}$'),

    CONSTRAINT statement_entries_bal_non_negative
        CHECK (bal_value_minor >= 0),

    CONSTRAINT statement_entries_bal_ccy_uppercase
        CHECK (bal_ccy ~ '^[A-Z]{3}$'),

    CONSTRAINT statement_entries_schema_version_v1
        CHECK (schema_version = 1)
);

COMMENT ON TABLE statement_entries IS 'Append-only camt.053 Ntry rows; immutable after insert';
COMMENT ON COLUMN statement_entries.amt_value_minor IS 'Positive magnitude; direction via cdt_dbt_ind';
COMMENT ON COLUMN statement_entries.end_to_end_id IS 'Denormalized from payment for support search';

CREATE INDEX idx_statement_entries_acct_cre_dt_tm_ntry_ref
    ON statement_entries (acct_id, cre_dt_tm DESC, ntry_ref DESC);

CREATE INDEX idx_statement_entries_end_to_end_id
    ON statement_entries (end_to_end_id);

CREATE INDEX idx_statement_entries_tx_id
    ON statement_entries (tx_id);

COMMIT;
