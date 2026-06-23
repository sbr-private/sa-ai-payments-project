import { Router } from 'express';
import { requireAuth, requireRole } from '../middleware/auth.js';
import {
  ApiError,
  createPaymentInitiation,
  getAccount,
  getHealth,
  getStatement,
} from '../lib/ledgerClient.js';
import { config } from '../config.js';

const router = Router();

router.use(requireAuth, requireRole('payer'));

router.get('/', async (req, res, next) => {
  try {
    const accountId = req.session.user.accountIds[0];
    const [health, account, statement] = await Promise.all([
      getHealth().catch(() => null),
      getAccount(req.session.user.email, accountId),
      getStatement(req.session.user.email, accountId, { limit: 10 }),
    ]);

    res.render('payer/dashboard', {
      title: 'Payer Portal',
      health,
      account,
      statement,
      accountId,
    });
  } catch (error) {
    next(error);
  }
});

router.get('/payment/new', async (req, res, next) => {
  try {
    const accountId = req.session.user.accountIds[0];
    const account = await getAccount(req.session.user.email, accountId);
    res.render('payer/payment', {
      title: 'New payment',
      account,
      supplierAccountId: config.seed.supplierAccountId,
      defaults: {
        endToEndId: `E2E-${new Date().toISOString().slice(0, 10).replace(/-/g, '')}-${String(Date.now()).slice(-4)}`,
        amount: '50.00',
        remittance: 'Invoice payment',
      },
      result: null,
      error: null,
    });
  } catch (error) {
    next(error);
  }
});

router.post('/payment/new', async (req, res, next) => {
  const accountId = req.session.user.accountIds[0];
  const endToEndId = String(req.body.endToEndId || '').trim();
  const amount = String(req.body.amount || '').trim();
  const remittance = String(req.body.remittance || '').trim();
  const creditorAccountId = String(req.body.creditorAccountId || config.seed.supplierAccountId).trim();

  try {
    const account = await getAccount(req.session.user.email, accountId);
    const now = new Date().toISOString();
    const msgId = `MSG-${now.slice(0, 10).replace(/-/g, '')}-${Date.now().toString().slice(-4)}`;
    const pmtInfId = `PMT-${now.slice(0, 10).replace(/-/g, '')}-${Date.now().toString().slice(-4)}`;

    const initiation = {
      grpHdr: {
        msgId,
        creDtTm: now,
        nbOfTxs: '1',
        ctrlSum: amount,
        initgPty: { nm: account.owner.nm },
      },
      pmtInf: [
        {
          pmtInfId,
          pmtMtd: 'TRF',
          dbtr: { nm: account.owner.nm },
          dbtrAcct: {
            id: { othr: { id: accountId } },
            ccy: account.ccy,
          },
          cdtTrfTxInf: [
            {
              pmtId: {
                instrId: `INSTR-${Date.now().toString().slice(-6)}`,
                endToEndId,
              },
              amt: {
                instdAmt: { value: amount, ccy: account.ccy },
              },
              cdtr: { nm: 'Supplier Ltd' },
              cdtrAcct: {
                id: { othr: { id: creditorAccountId } },
                ccy: account.ccy,
              },
              rmtInf: remittance ? { ustrd: [remittance] } : undefined,
            },
          ],
        },
      ],
    };

    const { body: result } = await createPaymentInitiation(req.session.user.email, initiation);

    res.render('payer/payment', {
      title: 'New payment',
      account,
      supplierAccountId: config.seed.supplierAccountId,
      defaults: { endToEndId, amount, remittance },
      result,
      error: null,
    });
  } catch (error) {
    const account = await getAccount(req.session.user.email, accountId).catch(() => null);
    const message = error instanceof ApiError ? error.message : 'Payment submission failed';
    res.status(error instanceof ApiError ? error.status : 500).render('payer/payment', {
      title: 'New payment',
      account,
      supplierAccountId: config.seed.supplierAccountId,
      defaults: { endToEndId, amount, remittance },
      result: null,
      error: message,
    });
  }
});

export default router;
