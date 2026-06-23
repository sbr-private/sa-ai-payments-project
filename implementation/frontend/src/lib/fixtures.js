import fs from 'node:fs/promises';
import path from 'node:path';
import { config } from '../config.js';

async function readFixture(name) {
  const filePath = path.join(config.fixturesPath, name);
  const raw = await fs.readFile(filePath, 'utf8');
  return JSON.parse(raw);
}

export async function getFixtureAccount(accountId) {
  const account = await readFixture('get-account-response.json');
  if (accountId === config.seed.acmeAccountId) {
    return { ...account, bal: { value: '950.00', ccy: 'USD' } };
  }
  if (accountId === config.seed.supplierAccountId) {
    return {
      ...account,
      id: config.seed.supplierAccountId,
      owner: { nm: 'Supplier Ltd', id: { othr: { id: 'supplier_001' } } },
      bal: { value: '50.00', ccy: 'USD' },
    };
  }
  return null;
}

export async function getFixtureStatement(accountId) {
  const statement = await readFixture('camt053-statement.json');
  if (accountId !== config.seed.acmeAccountId) {
    return { stmt: { ...statement.stmt, ntry: [] }, nextCursor: null, hasMore: false };
  }
  return statement;
}

export async function getFixtureTransaction(endToEndId) {
  if (endToEndId === config.seed.demoE2eRejected) {
    const pain002 = await readFixture('pain002-status-rjct.json');
    const tx = pain002.orgnlPmtInfAndSts[0].txInfAndSts[0];
    return { ...tx, orgnlEndToEndId: endToEndId };
  }
  if (endToEndId === config.seed.demoE2eSuccess) {
    return {
      orgnlEndToEndId: endToEndId,
      txSts: 'ACSC',
    };
  }
  return null;
}

export async function postFixturePayment(body) {
  const tx = body?.pmtInf?.[0]?.cdtTrfTxInf?.[0];
  const endToEndId = tx?.pmtId?.endToEndId;
  const amount = Number(tx?.amt?.instdAmt?.value ?? 0);

  if (!endToEndId) {
    const error = new Error('Validation failed');
    error.status = 400;
    error.body = { error: { code: 'VALIDATION_ERROR', message: 'endToEndId is required' } };
    throw error;
  }

  if (amount > 950) {
    const pain002 = await readFixture('pain002-status-rjct.json');
    return {
      status: 201,
      body: {
        ...pain002,
        orgnlPmtInfAndSts: [
          {
            orgnlPmtInfId: body.pmtInf[0].pmtInfId,
            txInfAndSts: [
              {
                orgnlEndToEndId: endToEndId,
                txSts: 'RJCT',
                stsRsnInf: [{ rsn: { cd: 'AM04' }, addtlInf: ['Insufficient funds on debtor account'] }],
              },
            ],
          },
        ],
      },
    };
  }

  const acsc = await readFixture('pain002-status-acsc.json');
  return {
    status: 201,
    body: {
      ...acsc,
      orgnlPmtInfAndSts: [
        {
          orgnlPmtInfId: body.pmtInf[0].pmtInfId,
          txInfAndSts: [{ orgnlEndToEndId: endToEndId, txSts: 'ACSC' }],
        },
      ],
    },
  };
}
