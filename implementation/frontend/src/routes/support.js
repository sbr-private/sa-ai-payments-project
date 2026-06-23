import { Router } from 'express';
import { requireAuth, requireRole } from '../middleware/auth.js';
import { getAccount, getStatement, getTransactionStatus } from '../lib/ledgerClient.js';
import { config } from '../config.js';

const router = Router();

router.use(requireAuth, requireRole('support'));

router.get('/', (req, res) => {
  res.render('support/search', {
    title: 'Control Centre',
    endToEndId: '',
    result: null,
    error: null,
    hints: [
      { id: config.seed.demoE2eSuccess, label: 'Successful payment (ACSC)' },
      { id: config.seed.demoE2eRejected, label: 'Rejected payment (RJCT / AM04)' },
    ],
  });
});

router.get('/search', async (req, res, next) => {
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
      hints: [
        { id: config.seed.demoE2eSuccess, label: 'Successful payment (ACSC)' },
        { id: config.seed.demoE2eRejected, label: 'Rejected payment (RJCT / AM04)' },
      ],
    });
  } catch (error) {
    res.status(error.status || 500).render('support/search', {
      title: 'Control Centre',
      endToEndId,
      result: null,
      error: error.message,
      hints: [
        { id: config.seed.demoE2eSuccess, label: 'Successful payment (ACSC)' },
        { id: config.seed.demoE2eRejected, label: 'Rejected payment (RJCT / AM04)' },
      ],
    });
  }
});

router.get('/accounts/:id', async (req, res, next) => {
  try {
    const accountId = req.params.id;
    const [account, statement] = await Promise.all([
      getAccount(req.session.user.email, accountId),
      getStatement(req.session.user.email, accountId, { limit: 20 }),
    ]);

    res.render('support/account', {
      title: `Account ${account.owner.nm}`,
      account,
      statement,
    });
  } catch (error) {
    next(error);
  }
});

export default router;
