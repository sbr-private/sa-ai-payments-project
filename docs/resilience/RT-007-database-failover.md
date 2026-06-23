# RT-007: Database Node Failover

**Operation:** OP-003, OP-002  
**Type:** Database HA (optional)  
**Required:** No — skip if environment lacks replica set or PostgreSQL standby

## Given

- **MongoDB:** 3-member replica set, or **PostgreSQL:** primary + synchronous/async standby (Patroni, manual promotion, or cloud HA)
- API configured with connection string that survives primary change (replica set URI, Patroni VIP, etc.)
- Funded accounts; baseline balances recorded

## When

1. Submit a transfer `endToEndId = E2E-RT007-PRE`, `instdAmt = { "value": "5.00", "ccy": "USD" }`; confirm `ACSC`.
2. Trigger **primary failover**:
   - MongoDB: `rs.stepDown()` on primary or `docker stop` primary member
   - PostgreSQL: `pg_ctl promote` on standby or kill primary container
3. Wait for election / promotion to complete (document duration).
4. Confirm `GET /ready` → `200` (may require brief unavailability window).
5. Submit a second transfer `endToEndId = E2E-RT007-POST`, `instdAmt = { "value": "5.00", "ccy": "USD" }`.
6. Read pre-failover transaction status and balances.

## Then

- Pre-failover `E2E-RT007-PRE` remains `ACSC` with correct balances.
- Post-failover `E2E-RT007-POST` settles `ACSC`.
- No lost or duplicated `ntry` records across failover.
- Document **unavailability window** (seconds from primary loss to first successful write).

## If skipped

Record: `skipped`, reason (e.g. single-node MongoDB, no Postgres replica), and adapter under test. RT-007 is not required for training completion.

## Comparison note

Postgres and Mongo failover mechanics differ. Compare **invariant preservation** and **recovery time**, not identical failover semantics.
