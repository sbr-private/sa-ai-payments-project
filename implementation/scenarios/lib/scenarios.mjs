import {
  ApiClient,
  accountBalance,
  countEntries,
  entriesForEndToEndId,
  entryRefs,
  reasonCode,
  txStatus,
} from "./client.mjs";

const MISSING_ACCOUNT = "00000000-0000-0000-0000-000000000000";

function assert(condition, message) {
  if (!condition) {
    throw new Error(message);
  }
}

function assertEq(actual, expected, label) {
  if (actual !== expected) {
    throw new Error(`${label}: expected ${JSON.stringify(expected)}, got ${JSON.stringify(actual)}`);
  }
}

function uniqueId(prefix) {
  return `${prefix}-${Date.now().toString(36)}`;
}

export const SCENARIOS = {
  "SC-001": {
    title: "Register account",
    async run(client) {
      const { json } = await client.createAccount({
        ownerName: "Acme Corp",
        ownerId: "user_123",
        ccy: "USD",
      });

      assertEq(json.bal.value, "0.00", "balance");
      assertEq(json.bal.ccy, "USD", "currency");
      assertEq(json.status, "active", "status");
      assert(
        /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(json.id),
        "id is UUID"
      );
      assert(!Number.isNaN(Date.parse(json.creDtTm)), "creDtTm is ISO-8601");
    },
  },

  "SC-002": {
    title: "Credit transfer (pain.001)",
    async run(client) {
      const debtor = (await client.createAccount({ ownerName: "Debtor SC2" })).json.id;
      const creditor = (await client.createAccount({ ownerName: "Creditor SC2" })).json.id;
      const e2e = uniqueId("E2E-SC002");

      await client.creditAccount(debtor, "1000.00", uniqueId("E2E-SEED-SC002"));

      const payment = await client.postPayment({
        debtorId: debtor,
        creditorId: creditor,
        amount: "50.00",
        endToEndId: e2e,
      });
      assertEq(payment.status, 201, "HTTP status");

      const tx = txStatus(payment);
      assertEq(tx.txSts, "ACSC", "txSts");
      assertEq(payment.json.orgnlGrpInfAndSts.grpSts, "ACSC", "grpSts");

      assertEq(accountBalance(await client.getAccount(debtor)), "950.00", "debtor balance");
      assertEq(accountBalance(await client.getAccount(creditor)), "50.00", "creditor balance");

      const debtorStmt = await client.getStatement(debtor);
      const creditorStmt = await client.getStatement(creditor);
      const debit = entriesForEndToEndId(debtorStmt, e2e)[0];
      const credit = entriesForEndToEndId(creditorStmt, e2e)[0];
      assert(debit?.cdtDbtInd === "DBIT" && debit.amt.value === "50.00", "debtor DBIT");
      assert(credit?.cdtDbtInd === "CRDT" && credit.amt.value === "50.00", "creditor CRDT");
    },
  },

  "SC-003": {
    title: "Insufficient funds (AM04)",
    async run(client) {
      const debtor = (await client.createAccount({ ownerName: "Debtor SC3" })).json.id;
      const creditor = (await client.createAccount({ ownerName: "Creditor SC3" })).json.id;
      const e2e = uniqueId("E2E-SC003");

      await client.creditAccount(debtor, "10.00", uniqueId("E2E-SEED-SC003"));

      const payment = await client.postPayment({
        debtorId: debtor,
        creditorId: creditor,
        amount: "50.00",
        endToEndId: e2e,
      });
      assertEq(payment.status, 201, "HTTP status");

      const tx = txStatus(payment);
      assertEq(tx.txSts, "RJCT", "txSts");
      assertEq(reasonCode(tx), "AM04", "reason");

      assertEq(accountBalance(await client.getAccount(debtor)), "10.00", "debtor balance");
      assertEq(accountBalance(await client.getAccount(creditor)), "0.00", "creditor balance");
      assertEq(entriesForEndToEndId(await client.getStatement(debtor), e2e).length, 0, "debtor ntry");
      assertEq(entriesForEndToEndId(await client.getStatement(creditor), e2e).length, 0, "creditor ntry");
    },
  },

  "SC-004": {
    title: "EndToEndId replay",
    async run(client) {
      const debtor = (await client.createAccount({ ownerName: "Debtor SC4" })).json.id;
      const creditor = (await client.createAccount({ ownerName: "Creditor SC4" })).json.id;
      const e2e = uniqueId("E2E-SC004");

      await client.creditAccount(debtor, "1000.00", uniqueId("E2E-SEED-SC004"));

      const body = {
        debtorId: debtor,
        creditorId: creditor,
        amount: "30.00",
        endToEndId: e2e,
      };

      const first = await client.postPayment(body);
      assertEq(first.status, 201, "first HTTP status");
      assertEq(txStatus(first).txSts, "ACSC", "first txSts");

      const replay = await client.postPayment(body);
      assertEq(replay.status, 200, "replay HTTP status");
      assertEq(txStatus(replay).txSts, "ACSC", "replay txSts");

      assertEq(accountBalance(await client.getAccount(debtor)), "970.00", "debtor balance");
      assertEq(accountBalance(await client.getAccount(creditor)), "30.00", "creditor balance");

      const transferEntries =
        entriesForEndToEndId(await client.getStatement(debtor), e2e).length +
        entriesForEndToEndId(await client.getStatement(creditor), e2e).length;
      assertEq(transferEntries, 2, "transfer ntry count");
    },
  },

  "SC-005": {
    title: "EndToEndId conflict (DU04)",
    async run(client) {
      const debtor = (await client.createAccount({ ownerName: "Debtor SC5" })).json.id;
      const creditorB = (await client.createAccount({ ownerName: "Creditor B SC5" })).json.id;
      const creditorC = (await client.createAccount({ ownerName: "Creditor C SC5" })).json.id;
      const e2e = uniqueId("E2E-SC005");

      await client.creditAccount(debtor, "1000.00", uniqueId("E2E-SEED-SC005"));

      await client.postPayment({
        debtorId: debtor,
        creditorId: creditorB,
        amount: "20.00",
        endToEndId: e2e,
      });

      const conflict = await client.postPayment({
        debtorId: debtor,
        creditorId: creditorC,
        amount: "20.00",
        endToEndId: e2e,
      });
      assertEq(conflict.status, 409, "HTTP status");
      assertEq(txStatus(conflict).txSts, "RJCT", "txSts");
      assertEq(reasonCode(txStatus(conflict)), "DU04", "reason");

      assertEq(accountBalance(await client.getAccount(debtor)), "980.00", "debtor balance");
      assertEq(accountBalance(await client.getAccount(creditorB)), "20.00", "creditor B balance");
      assertEq(accountBalance(await client.getAccount(creditorC)), "0.00", "creditor C balance");
    },
  },

  "SC-006": {
    title: "Currency mismatch (CURR)",
    async run(client) {
      const usd = (await client.createAccount({ ownerName: "USD SC6", ccy: "USD" })).json.id;
      const eur = (await client.createAccount({ ownerName: "EUR SC6", ccy: "EUR" })).json.id;

      await client.creditAccount(usd, "500.00", uniqueId("E2E-SEED-SC006-USD"), "USD");

      const payment = await client.postPayment({
        debtorId: usd,
        creditorId: eur,
        amount: "10.00",
        endToEndId: uniqueId("E2E-SC006"),
        instdCcy: "USD",
        debtorCcy: "USD",
        creditorCcy: "EUR",
      });
      assertEq(payment.status, 201, "HTTP status");
      assertEq(txStatus(payment).txSts, "RJCT", "txSts");
      assertEq(reasonCode(txStatus(payment)), "CURR", "reason");

      assertEq(accountBalance(await client.getAccount(usd)), "500.00", "USD balance");
      assertEq(accountBalance(await client.getAccount(eur)), "0.00", "EUR balance");
    },
  },

  "SC-007": {
    title: "Closed account (AC04)",
    async run(client) {
      const closed = (await client.createAccount({ ownerName: "Closed SC7" })).json.id;
      const active = (await client.createAccount({ ownerName: "Active SC7" })).json.id;

      await client.closeAccount(closed);

      const fromClosed = await client.postPayment({
        debtorId: closed,
        creditorId: active,
        amount: "10.00",
        endToEndId: uniqueId("E2E-SC007-A"),
      });
      assertEq(txStatus(fromClosed).txSts, "RJCT", "debtor closed txSts");
      assertEq(reasonCode(txStatus(fromClosed)), "AC04", "debtor closed reason");
      assertEq(accountBalance(await client.getAccount(closed)), "0.00", "closed balance");
      assertEq(accountBalance(await client.getAccount(active)), "0.00", "active balance after from-closed");

      await client.creditAccount(active, "100.00", uniqueId("E2E-SEED-SC007-A"));
      const toClosed = await client.postPayment({
        debtorId: active,
        creditorId: closed,
        amount: "10.00",
        endToEndId: uniqueId("E2E-SC007-B"),
      });
      assertEq(txStatus(toClosed).txSts, "RJCT", "creditor closed txSts");
      assertEq(reasonCode(txStatus(toClosed)), "AC04", "creditor closed reason");
      assertEq(accountBalance(await client.getAccount(active)), "100.00", "active balance after to-closed");
      assertEq(accountBalance(await client.getAccount(closed)), "0.00", "closed balance unchanged");
    },
  },

  "SC-008": {
    title: "Double-entry invariant",
    async run(client) {
      const debtor = (await client.createAccount({ ownerName: "Debtor SC8" })).json.id;
      const creditor = (await client.createAccount({ ownerName: "Creditor SC8" })).json.id;
      const e2e = uniqueId("E2E-SC008");

      await client.creditAccount(debtor, "500.00", uniqueId("E2E-SEED-SC008-D"));
      await client.creditAccount(creditor, "100.00", uniqueId("E2E-SEED-SC008-C"));

      const payment = await client.postPayment({
        debtorId: debtor,
        creditorId: creditor,
        amount: "75.00",
        endToEndId: e2e,
      });
      assertEq(txStatus(payment).txSts, "ACSC", "txSts");

      const debit = entriesForEndToEndId(await client.getStatement(debtor), e2e);
      const credit = entriesForEndToEndId(await client.getStatement(creditor), e2e);
      assertEq(debit.length, 1, "debit entries");
      assertEq(credit.length, 1, "credit entries");
      assertEq(debit[0].cdtDbtInd, "DBIT", "debit indicator");
      assertEq(credit[0].cdtDbtInd, "CRDT", "credit indicator");
      assertEq(debit[0].amt.value, "75.00", "debit amount");
      assertEq(credit[0].amt.value, "75.00", "credit amount");
    },
  },

  "SC-009": {
    title: "Balance integrity",
    async run(client) {
      const accA = (await client.createAccount({ ownerName: "Account A SC9" })).json.id;
      const accB = (await client.createAccount({ ownerName: "Account B SC9" })).json.id;

      await client.creditAccount(accA, "500.00", uniqueId("E2E-SEED-SC009"));

      await client.postPayment({
        debtorId: accA,
        creditorId: accB,
        amount: "150.00",
        endToEndId: uniqueId("E2E-SC009-1"),
      });
      await client.postPayment({
        debtorId: accB,
        creditorId: accA,
        amount: "50.00",
        endToEndId: uniqueId("E2E-SC009-2"),
      });
      await client.postPayment({
        debtorId: accA,
        creditorId: accB,
        amount: "80.00",
        endToEndId: uniqueId("E2E-SC009-3"),
      });

      assertEq(accountBalance(await client.getAccount(accA)), "320.00", "acc_a balance");
      assertEq(accountBalance(await client.getAccount(accB)), "180.00", "acc_b balance");

      for (const id of [accA, accB]) {
        const stmt = await client.getStatement(id, { limit: 100 });
        const closing = stmt.json.stmt.bal.find((b) => b.tp.cdOrPrtry.cd === "CLBD");
        assert(closing, "CLBD balance present");
        assertEq(closing.amt.value, accountBalance(await client.getAccount(id)), "CLBD matches bal");
      }
    },
  },

  "SC-010": {
    title: "Concurrent transfers",
    async run(client) {
      const debtor = (await client.createAccount({ ownerName: "Debtor SC10" })).json.id;
      const creditor = (await client.createAccount({ ownerName: "Creditor SC10" })).json.id;

      await client.creditAccount(debtor, "100.00", uniqueId("E2E-SEED-SC010"));

      const e2eA = uniqueId("E2E-SC10-A");
      const e2eB = uniqueId("E2E-SC10-B");
      const body = (e2e) => ({
        debtorId: debtor,
        creditorId: creditor,
        amount: "80.00",
        endToEndId: e2e,
      });

      const [resA, resB] = await Promise.all([
        client.postPayment(body(e2eA)),
        client.postPayment(body(e2eB)),
      ]);

      const statuses = [txStatus(resA), txStatus(resB)];
      const acsc = statuses.filter((tx) => tx.txSts === "ACSC").length;
      const rjct = statuses.filter((tx) => tx.txSts === "RJCT").length;
      assertEq(acsc, 1, "ACSC count");
      assertEq(rjct, 1, "RJCT count");
      assert(
        statuses.some((tx) => tx.txSts === "RJCT" && reasonCode(tx) === "AM04"),
        "rejection is AM04"
      );

      assertEq(accountBalance(await client.getAccount(debtor)), "20.00", "debtor balance");
      assertEq(accountBalance(await client.getAccount(creditor)), "80.00", "creditor balance");

      const debtorStmt = await client.getStatement(debtor, { limit: 100 });
      const creditorStmt = await client.getStatement(creditor, { limit: 100 });
      const debtorPayments = [e2eA, e2eB].flatMap((id) =>
        entriesForEndToEndId(debtorStmt, id)
      );
      const creditorPayments = [e2eA, e2eB].flatMap((id) =>
        entriesForEndToEndId(creditorStmt, id)
      );
      assertEq(debtorPayments.length, 1, "debtor payment entries");
      assertEq(creditorPayments.length, 1, "creditor payment entries");

      // Variant: concurrent same EndToEndId
      const debtor2 = (await client.createAccount({ ownerName: "Debtor SC10b" })).json.id;
      const creditor2 = (await client.createAccount({ ownerName: "Creditor SC10b" })).json.id;
      await client.creditAccount(debtor2, "100.00", uniqueId("E2E-SEED-SC10b"));
      const sameE2e = uniqueId("E2E-SC10-SAME");
      const sameBody = body(sameE2e);
      sameBody.debtorId = debtor2;
      sameBody.creditorId = creditor2;

      const [sameA, sameB] = await Promise.all([
        client.postPayment(sameBody),
        client.postPayment(sameBody),
      ]);
      assert(txStatus(sameA).txSts === "ACSC" && txStatus(sameB).txSts === "ACSC", "both ACSC");
      assertEq(txStatus(sameA).orgnlEndToEndId, sameE2e, "same orgnlEndToEndId A");
      assertEq(txStatus(sameB).orgnlEndToEndId, sameE2e, "same orgnlEndToEndId B");
      assertEq(accountBalance(await client.getAccount(debtor2)), "20.00", "same-id debtor balance");
      assertEq(accountBalance(await client.getAccount(creditor2)), "80.00", "same-id creditor balance");
    },
  },

  "SC-011": {
    title: "Statement pagination",
    async run(client) {
      const debtor = (await client.createAccount({ ownerName: "Debtor SC11" })).json.id;
      const creditor = (await client.createAccount({ ownerName: "Creditor SC11" })).json.id;

      await client.creditAccount(debtor, "200.00", uniqueId("E2E-SEED-SC011"));

      for (let i = 0; i < 5; i++) {
        await client.postPayment({
          debtorId: debtor,
          creditorId: creditor,
          amount: "10.00",
          endToEndId: uniqueId(`E2E-SC011-${i}`),
        });
      }

      const page1 = await client.getStatement(debtor, { limit: 2 });
      assertEq(countEntries(page1), 2, "page1 count");
      assertEq(page1.json.hasMore, true, "page1 hasMore");
      assert(page1.json.nextCursor, "page1 cursor");

      const seen = new Set(entryRefs(page1));
      const page2 = await client.getStatement(debtor, {
        limit: 2,
        cursor: page1.json.nextCursor,
      });
      assertEq(countEntries(page2), 2, "page2 count");
      assertEq(page2.json.hasMore, true, "page2 hasMore");
      for (const ref of entryRefs(page2)) {
        assert(!seen.has(ref), `page2 overlap: ${ref}`);
        seen.add(ref);
      }

      const page3 = await client.getStatement(debtor, {
        limit: 2,
        cursor: page2.json.nextCursor,
      });
      assert(countEntries(page3) >= 1, "page3 has remaining entries");
      assertEq(page3.json.hasMore, false, "page3 hasMore");
      for (const ref of entryRefs(page3)) {
        assert(!seen.has(ref), `page3 overlap: ${ref}`);
      }

      const badLow = await client.getStatement(debtor, { limit: 0 });
      assertEq(badLow.status, 400, "limit=0 status");
      assertEq(badLow.json.error.code, "VALIDATION_ERROR", "limit=0 code");

      const badHigh = await client.getStatement(debtor, { limit: 101 });
      assertEq(badHigh.status, 400, "limit=101 status");
      assertEq(badHigh.json.error.code, "VALIDATION_ERROR", "limit=101 code");
    },
  },

  "SC-012": {
    title: "Transaction not found",
    async run(client) {
      const res = await client.getTransaction("E2E-MISSING-0001");
      assertEq(res.status, 404, "HTTP status");
      assertEq(res.json.error.details.endToEndId, "E2E-MISSING-0001", "endToEndId in error");
    },
  },

  "SC-013": {
    title: "Account not found",
    async run(client) {
      const getRes = await client.getAccount(MISSING_ACCOUNT);
      assertEq(getRes.status, 404, "GET account status");

      const stmtRes = await client.getStatement(MISSING_ACCOUNT);
      assertEq(stmtRes.status, 404, "GET statement status");

      const payment = await client.postPayment({
        debtorId: MISSING_ACCOUNT,
        creditorId: (await client.createAccount({ ownerName: "Creditor SC13" })).json.id,
        amount: "10.00",
        endToEndId: uniqueId("E2E-SC013"),
      });
      assertEq(payment.status, 201, "payment HTTP status");
      assertEq(txStatus(payment).txSts, "RJCT", "txSts");
      assertEq(reasonCode(txStatus(payment)), "BE01", "reason");
    },
  },

  "SC-014": {
    title: "Invalid amount (AM12)",
    async run(client) {
      const debtor = (await client.createAccount({ ownerName: "Debtor SC14" })).json.id;
      const creditor = (await client.createAccount({ ownerName: "Creditor SC14" })).json.id;
      await client.creditAccount(debtor, "100.00", uniqueId("E2E-SEED-SC014"));

      for (const amount of ["0.00", "-10.00", "10.001"]) {
        const payment = await client.postPayment({
          debtorId: debtor,
          creditorId: creditor,
          amount,
          endToEndId: uniqueId("E2E-SC014"),
        });
        assertEq(payment.status, 201, `HTTP status for ${amount}`);
        assertEq(txStatus(payment).txSts, "RJCT", `txSts for ${amount}`);
        assertEq(reasonCode(txStatus(payment)), "AM12", `reason for ${amount}`);
      }

      assertEq(accountBalance(await client.getAccount(debtor)), "100.00", "balance unchanged");

      const missingE2e = await client.postPayment({
        debtorId: debtor,
        creditorId: creditor,
        amount: "10.00",
        endToEndId: uniqueId("E2E-SC014-NO-E2E"),
        omitEndToEndId: true,
      });
      assertEq(missingE2e.status, 400, "missing endToEndId status");
      assertEq(missingE2e.json.error.code, "VALIDATION_ERROR", "missing endToEndId code");
    },
  },

  "SC-015": {
    title: "Self-transfer (AG01)",
    async run(client) {
      const account = (await client.createAccount({ ownerName: "Self SC15" })).json.id;
      const e2e = uniqueId("E2E-SC015");
      await client.creditAccount(account, "100.00", uniqueId("E2E-SEED-SC015"));

      const payment = await client.postPayment({
        debtorId: account,
        creditorId: account,
        amount: "10.00",
        endToEndId: e2e,
      });
      assertEq(payment.status, 201, "HTTP status");
      assertEq(txStatus(payment).txSts, "RJCT", "txSts");
      assertEq(reasonCode(txStatus(payment)), "AG01", "reason");
      assertEq(accountBalance(await client.getAccount(account)), "100.00", "balance");
      assertEq(entriesForEndToEndId(await client.getStatement(account), e2e).length, 0, "no ntry");
    },
  },
};

export async function runScenario(client, id) {
  const scenario = SCENARIOS[id];
  if (!scenario) {
    throw new Error(`Unknown scenario: ${id}`);
  }
  await scenario.run(client);
  return scenario.title;
}

export async function runAllScenarios(client, filter) {
  const ids = filter?.length
    ? filter
    : Object.keys(SCENARIOS).sort();
  const results = [];

  for (const id of ids) {
    const started = Date.now();
    try {
      const title = await runScenario(client, id);
      results.push({ id, title, ok: true, ms: Date.now() - started });
    } catch (err) {
      results.push({
        id,
        title: SCENARIOS[id]?.title ?? id,
        ok: false,
        ms: Date.now() - started,
        error: err.message,
      });
    }
  }

  return results;
}
