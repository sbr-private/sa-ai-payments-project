import { Router } from 'express';
import { requireAuth, requireRole } from '../middleware/auth.js';
import {
  ApiError,
  createPaymentInitiation,
  getAccount,
  getHealth,
  getReady,
  getStatement,
  getTransactionStatus,
  isTransactionLookupUnavailable,
} from '../lib/ledgerClient.js';
import { config } from '../config.js';
import { extractTxStatus } from '../lib/demo.js';

const router = Router();

router.use(requireAuth, requireRole('payer'));

router.get('/', async (req, res, next) => {
  const accountId = req.session.user.accountIds[0];
  const cursor = String(req.query.cursor || '').trim() || undefined;
  const paymentSuccess = req.query.payment === 'success';
  const paymentEndToEndId = String(req.query.endToEndId || '').trim();

  try {
    const [health, ready, account, statement] = await Promise.all([
      getHealth().catch(() => null),
      getReady(req.session.user.email).catch(() => null),
      getAccount(req.session.user.email, accountId),
      getStatement(req.session.user.email, accountId, { limit: 10, cursor }),
    ]);

    res.render('payer/dashboard', {
      title: 'Payer Portal',
      health,
      ready,
      account,
      statement,
      accountId,
      cursor,
      paymentSuccess,
      paymentEndToEndId,
    });
  } catch (error) {
    if (error instanceof ApiError && error.status === 404) {
      return res.status(404).render('payer/setup-hint', {
        title: 'Account not found',
        accountId,
        user: req.session.user,
      });
    }
    next(error);
  }
});

router.get('/payments/status', async (req, res, next) => {
  const endToEndId = String(req.query.endToEndId || '').trim();

  if (!endToEndId) {
    return res.render('payer/status', {
      title: 'Payment status',
      endToEndId: '',
      result: null,
      error: null,
      lookupUnavailable: false,
    });
  }

  try {
    const result = await getTransactionStatus(req.session.user.email, endToEndId);
    res.render('payer/status', {
      title: 'Payment status',
      endToEndId,
      result,
      error: null,
      lookupUnavailable: false,
    });
  } catch (error) {
    const lookupUnavailable = isTransactionLookupUnavailable(error);
    res.status(error.status || 500).render('payer/status', {
      title: 'Payment status',
      endToEndId,
      result: null,
      error: lookupUnavailable
        ? 'Status lookup is not available on the ledger API yet. Use the pain.002 from your payment submission, or enable USE_FIXTURES=true.'
        : error.message,
      lookupUnavailable,
    });
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
    if (error instanceof ApiError && error.status === 404) {
      return res.status(404).render('payer/setup-hint', {
        title: 'Account not found',
        accountId: req.session.user.accountIds[0],
        user: req.session.user,
      });
    }
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
    const tx = extractTxStatus(result);

    if (tx?.txSts === 'ACSC') {
      return res.redirect(
        `/payer?payment=success&endToEndId=${encodeURIComponent(endToEndId)}`,
      );
    }

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
