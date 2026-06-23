# ISO 20022 Field Mapping

This document maps ISO 20022 XML elements to the JSON API binding used by this project, and describes internal storage conventions for database adapters.

**Target message versions:**

| Message | XSD identifier | Namespace |
|---------|----------------|-----------|
| pain.001 | `pain.001.001.09` | Customer Credit Transfer Initiation |
| pain.002 | `pain.002.001.10` | Customer Payment Status Report |
| camt.053 | `camt.053.001.08` | Bank-to-Customer Statement |

Official schemas: [ISO 20022 Message Repository](https://www.iso20022.org/iso-20022-message-definitions)

---

## JSON binding rules

1. XML element names → **camelCase** JSON properties (`EndToEndId` → `endToEndId`).
2. XML attributes → sibling properties (`<InstdAmt Ccy="USD">` → `{ "value": "...", "ccy": "USD" }`).
3. Amount `value` is always a **string** decimal in API payloads.
4. Optional ISO elements omitted when empty; do not send `null` for absent optional fields.

---

## pain.001 → API request

| ISO XPath | JSON path | Required | Notes |
|-----------|-----------|----------|-------|
| `/Document/CstmrCdtTrfInitn/GrpHdr/MsgId` | `grpHdr.msgId` | Yes | Client-generated message ID |
| `/GrpHdr/CreDtTm` | `grpHdr.creDtTm` | Yes | ISO 8601 |
| `/GrpHdr/NbOfTxs` | `grpHdr.nbOfTxs` | Yes | Must match transaction count |
| `/GrpHdr/CtrlSum` | `grpHdr.ctrlSum` | Yes | Sum of instructed amounts |
| `/GrpHdr/InitgPty/Nm` | `grpHdr.initgPty.nm` | Yes | Initiating party name |
| `/PmtInf/PmtInfId` | `pmtInf[].pmtInfId` | Yes | Payment info block ID |
| `/PmtInf/PmtMtd` | `pmtInf[].pmtMtd` | Yes | Always `TRF` (transfer) in v1 |
| `/PmtInf/Dbtr/Nm` | `pmtInf[].dbtr.nm` | Yes | Debtor name |
| `/PmtInf/DbtrAcct/Id/Othr/Id` | `pmtInf[].dbtrAcct.id.othr.id` | Yes | Debtor account UUID |
| `/PmtInf/DbtrAcct/Ccy` | `pmtInf[].dbtrAcct.ccy` | Yes | ISO 4217 |
| `/CdtTrfTxInf/PmtId/InstrId` | `cdtTrfTxInf[].pmtId.instrId` | No | Max 35 chars |
| `/CdtTrfTxInf/PmtId/EndToEndId` | `cdtTrfTxInf[].pmtId.endToEndId` | Yes | Idempotency key; max 35 chars |
| `/CdtTrfTxInf/Amt/InstdAmt` | `cdtTrfTxInf[].amt.instdAmt` | Yes | `{ value, ccy }` |
| `/CdtTrfTxInf/Cdtr/Nm` | `cdtTrfTxInf[].cdtr.nm` | Yes | Creditor name |
| `/CdtTrfTxInf/CdtrAcct/Id/Othr/Id` | `cdtTrfTxInf[].cdtrAcct.id.othr.id` | Yes | Creditor account UUID |
| `/CdtTrfTxInf/RmtInf/Ustrd` | `cdtTrfTxInf[].rmtInf.ustrd` | No | Unstructured remittance |

### v1 simplifications (not in request)

| Element | Status |
|---------|--------|
| `DbtrAgt`, `CdtrAgt` | Omitted — no agent routing in v1 |
| `ChrgBr` | Omitted — no charges |
| `EqvtAmt` | Omitted — no FX |
| `ReqdExctnDt` | Omitted — immediate execution |

---

## pain.002 → API response

| ISO XPath | JSON path | Notes |
|-----------|-----------|-------|
| `/GrpHdr/MsgId` | `grpHdr.msgId` | Server-generated status message ID |
| `/OrgnlGrpInfAndSts/OrgnlMsgId` | `orgnlGrpInfAndSts.orgnlMsgId` | Echoes pain.001 `msgId` |
| `/OrgnlGrpInfAndSts/GrpSts` | `orgnlGrpInfAndSts.grpSts` | `ACSC` or `RJCT` |
| `/TxInfAndSts/OrgnlEndToEndId` | `txInfAndSts[].orgnlEndToEndId` | Echoes `endToEndId` |
| `/TxInfAndSts/TxSts` | `txInfAndSts[].txSts` | `ACSC`, `RJCT` |
| `/TxInfAndSts/StsRsnInf/Rsn/Cd` | `txInfAndSts[].stsRsnInf[].rsn.cd` | Mandatory when `RJCT` |

### Status code mapping

| Internal event | `TxSts` | `Rsn.Cd` |
|----------------|---------|----------|
| Settled | `ACSC` | — |
| Insufficient funds | `RJCT` | `AM04` |
| Closed account | `RJCT` | `AC04` |
| Invalid amount | `RJCT` | `AM12` |
| Currency mismatch | `RJCT` | `CURR` |
| Duplicate E2E, different data | `RJCT` | `DU04` |
| Self-transfer | `RJCT` | `AG01` |
| Account not found | `RJCT` | `BE01` |

---

## camt.053 → statement response

| ISO XPath | JSON path | Notes |
|-----------|-----------|-------|
| `/Stmt/Id` | `stmt.id` | Statement page identifier |
| `/Stmt/Acct/Id/Othr/Id` | `stmt.acct.id.othr.id` | Account UUID |
| `/Stmt/Bal/Amt` | `stmt.bal[].amt` | Closing balance snapshot |
| `/Stmt/Bal/Tp/Cd` | `stmt.bal[].tp.cdOrPrtry.cd` | `CLBD` (closing booked) |
| `/Stmt/Ntry/NtryRef` | `stmt.ntry[].ntryRef` | Entry UUID |
| `/Stmt/Ntry/Amt` | `stmt.ntry[].amt` | Always positive magnitude |
| `/Stmt/Ntry/CdtDbtInd` | `stmt.ntry[].cdtDbtInd` | `CRDT` or `DBIT` |
| `/Stmt/Ntry/BookgDt/Dt` | `stmt.ntry[].bookgDt.dt` | Booking date |
| `/Ntry/NtryDtls/TxDtls/Refs/EndToEndId` | `ntry[].ntryDtls[].txDtls[].refs.endToEndId` | Payment reference |

### Debit/credit convention

Unlike the v1 internal ledger format, camt.053 `Ntry.Amt.value` is **always positive**. Direction is expressed solely via `CdtDbtInd`.

---

## Amount conversion (API ↔ storage)

### API → minor units (internal)

```
minor = round(parseDecimal(value) * 10^exponent)
```

Where `exponent` is ISO 4217 minor units (USD/EUR = 2, JPY = 0, BHD = 3).

Examples:

| API value | ccy | Internal minor |
|-----------|-----|----------------|
| `"50.00"` | USD | 5000 |
| `"1000"` | JPY | 1000 |
| `"0.50"` | USD | 50 |

### Internal → API

Format `minor` to exactly `exponent` decimal places (strip trailing zeros only if exponent is 0).

### Validation

Reject with `AM12` if `value` has more decimal places than the currency allows, or if `value ≤ 0`.

---

## Idempotency: EndToEndId

ISO 20022 defines `EndToEndId` as:

> Unique identification assigned by the initiating party to unambiguously identify the transaction. This identification is passed on, unchanged, throughout the entire end-to-end chain.

This replaces the generic `Idempotency-Key` header from v1. Constraints:

- Max 35 characters (`Max35Text`)
- Unique within the ledger scope
- Compared with full `CdtTrfTxInf` body for replay detection

---

## Database adapter notes

Adapters may normalize ISO JSON into relational/nested documents:

| ISO concept | Suggested PG table | Suggested Mongo collection |
|-------------|-------------------|---------------------------|
| Account | `accounts` | `accounts` |
| PaymentTransaction | `payment_transactions` | `payment_transactions` |
| StatementEntry | `statement_entries` | `statement_entries` |
| Idempotency index | unique on `end_to_end_id` | unique index on `pmtId.endToEndId` |

Physical schema details:

| Adapter | Document |
|---------|----------|
| MongoDB | [docs/adapters/mongodb/SCHEMA.md](../adapters/mongodb/SCHEMA.md) |
| PostgreSQL | [docs/adapters/postgres/SCHEMA.md](../adapters/postgres/SCHEMA.md) |

Demo seed data: [SEED.md](../SEED.md)

---

## Related documents

| Document | Role |
|----------|------|
| [SPEC.md](../SPEC.md) | Domain model and API |
| [SEED.md](../SEED.md) | Demo seed data |
| [AUTH.md](../AUTH.md) | Demo login (outside DB) |
| [PURPOSE.md](../PURPOSE.md) | Project mission |
| [../README.md](../../README.md) | Project entry point |
