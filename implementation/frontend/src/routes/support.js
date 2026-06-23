import { Router } from 'express';
import { requireAuth, requireRole } from '../middleware/auth.js';
import { getAccount, getStatement, getTransactionStatus, isTransactionLookupUnavailable } from '../lib/ledgerClient.js';
import { config } from '../config.js';
import { debtorAccountForE2e } from '../lib/demo.js';

const router = Router();

router.use(requireAuth, requireRole('support'));

const hints = [
  { id: config.seed.demoE2eSuccess, label: 'Successful payment (ACSC)' },
  { id: config.seed.demoE2eRejected, label: 'Rejected payment (RJCT / AM04)' },
];

router.get('/', (req, res) => {
  res.render('support/search', {
    title: 'Control Centre',
    endToEndId: '',
    result: null,
    error: null,
    lookupUnavailable: false,
    debtorAccountId: null,
    hints,
  });
});

router.get('/search', async (req, res) => {
  const endToEndId = String(req.query.endToEndId || '').trim();

  if (!endToEndId) {
    return res.redirect('/support');
  }

  try {
    const result = await getTransactionStatus(req.session.user.email, endToEndId);
    res.render('support/search', {
      title: 'Control Centre',
      endToEndId,
      result,
      error: null,
      lookupUnavailable: false,
      debtorAccountId: debtorAccountForE2e(endToEndId),
      hints,
    });
  } catch (error) {
    const lookupUnavailable = isTransactionLookupUnavailable(error);
    res.status(error.status || 500).render('support/search', {
      title: 'Control Centre',
      endToEndId,
      result: null,
      error: lookupUnavailable
        ? 'Transaction lookup is not available on the ledger API yet. Enable USE_FIXTURES=true or wait for GET /payment-initiations/transactions/{endToEndId}.'
        : error.message,
      lookupUnavailable,
      debtorAccountId: debtorAccountForE2e(endToEndId),
      hints,
    });
  }
});

router.get('/accounts/:id', async (req, res, next) => {
  try {
    const accountId = req.params.id;
    const cursor = String(req.query.cursor || '').trim() || undefined;
    const [account, statement] = await Promise.all([
      getAccount(req.session.user.email, accountId),
      getStatement(req.session.user.email, accountId, { limit: 10, cursor }),
    ]);

    res.render('support/account', {
      title: `Account ${account.owner.nm}`,
      account,
      statement,
      accountId,
      cursor,
    });
  } catch (error) {
    next(error);
  }
});

export default router;
