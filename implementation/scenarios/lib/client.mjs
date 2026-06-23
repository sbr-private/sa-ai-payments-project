const DEFAULT_BASE_URL = "http://localhost:8080/v1";
const DEFAULT_DEMO_USER = "benchmark@demo";

export class ApiClient {
  constructor(options = {}) {
    this.baseUrl = (options.baseUrl ?? DEFAULT_BASE_URL).replace(/\/$/, "");
    this.demoUser = options.demoUser ?? DEFAULT_DEMO_USER;
  }

  async request(method, path, { body, expectedStatus } = {}) {
    const headers = { "X-Demo-User": this.demoUser };
    const init = { method, headers };

    if (body !== undefined) {
      headers["Content-Type"] = "application/json";
      init.body = JSON.stringify(body);
    }

    const response = await fetch(`${this.baseUrl}${path}`, init);
    const text = await response.text();
    let json = null;
    if (text) {
      try {
        json = JSON.parse(text);
      } catch {
        json = text;
      }
    }

    if (expectedStatus !== undefined && response.status !== expectedStatus) {
      throw new Error(
        `${method} ${path} expected HTTP ${expectedStatus}, got ${response.status}: ${text}`
      );
    }

    return { status: response.status, json, text };
  }

  createAccount({ ownerName = "Scenario Co", ownerId = "scn_user", ccy = "USD" } = {}) {
    return this.request("POST", "/accounts", {
      expectedStatus: 201,
      body: {
        owner: {
          nm: ownerName,
          id: { othr: { id: ownerId } },
        },
        ccy,
      },
    });
  }

  getAccount(id) {
    return this.request("GET", `/accounts/${id}`);
  }

  creditAccount(id, amount, endToEndId, ccy = "USD") {
    return this.request("POST", `/test/accounts/${id}/credit`, {
      expectedStatus: 204,
      body: {
        amount: { value: amount, ccy },
        endToEndId,
      },
    });
  }

  closeAccount(id) {
    return this.request("POST", `/test/accounts/${id}/close`, { expectedStatus: 204 });
  }

  postPayment({
    debtorId,
    creditorId,
    amount,
    endToEndId,
    instdCcy = "USD",
    debtorCcy = "USD",
    creditorCcy = "USD",
    omitEndToEndId = false,
  }) {
    const pmtId = omitEndToEndId ? {} : { endToEndId };
    return this.request("POST", "/payment-initiations", {
      body: {
        grpHdr: {
          msgId: `MSG-${endToEndId ?? "NO-E2E"}`,
          creDtTm: "2026-06-23T12:00:00Z",
          nbOfTxs: "1",
          ctrlSum: amount,
          initgPty: { nm: "Scenario Runner" },
        },
        pmtInf: [
          {
            pmtInfId: `PMT-${endToEndId ?? "NO-E2E"}`,
            pmtMtd: "TRF",
            dbtr: { nm: "Debtor" },
            dbtrAcct: {
              id: { othr: { id: debtorId } },
              ccy: debtorCcy,
            },
            cdtTrfTxInf: [
              {
                pmtId,
                amt: { instdAmt: { value: amount, ccy: instdCcy } },
                cdtr: { nm: "Creditor" },
                cdtrAcct: {
                  id: { othr: { id: creditorId } },
                  ccy: creditorCcy,
                },
              },
            ],
          },
        ],
      },
    });
  }

  getStatement(id, { limit = 20, cursor } = {}) {
    const params = new URLSearchParams({ limit: String(limit) });
    if (cursor) {
      params.set("cursor", cursor);
    }
    return this.request("GET", `/accounts/${id}/statements?${params}`);
  }

  getTransaction(endToEndId) {
    return this.request("GET", `/payment-initiations/transactions/${endToEndId}`);
  }

  ready() {
    return this.request("GET", "/ready");
  }
}

export function txStatus(response) {
  return response.json?.orgnlPmtInfAndSts?.[0]?.txInfAndSts?.[0] ?? null;
}

export function reasonCode(tx) {
  return tx?.stsRsnInf?.[0]?.rsn?.cd ?? null;
}

export function accountBalance(response) {
  return response.json?.bal?.value ?? null;
}

export function entriesForEndToEndId(statementResponse, endToEndId) {
  const entries = statementResponse.json?.stmt?.ntry ?? [];
  return entries.filter((entry) =>
    entry.ntryDtls?.some((detail) =>
      detail.txDtls?.some((tx) => tx.refs?.endToEndId === endToEndId)
    )
  );
}

export function countEntries(statementResponse) {
  return statementResponse.json?.stmt?.ntry?.length ?? 0;
}

export function entryRefs(statementResponse) {
  return (statementResponse.json?.stmt?.ntry ?? []).map((e) => e.ntryRef);
}
