import { Router } from 'express';
import { login, ApiError } from '../lib/ledgerClient.js';
import { config } from '../config.js';

const router = Router();

function portalForRole(role) {
  if (role === 'support') return '/support';
  if (role === 'payer') return '/payer';
  return '/login';
}

router.get('/login', (req, res) => {
  if (req.session?.user) {
    return res.redirect(portalForRole(req.session.user.role));
  }
  res.render('login', {
    title: 'Sign in',
    error: null,
    email: '',
    apiUrl: config.apiUrl,
    useFixtures: config.useFixtures,
  });
});

router.post('/login', async (req, res) => {
  const email = String(req.body.email || '').trim();
  const password = String(req.body.password || '');

  try {
    const user = await login(email, password);
    req.session.user = user;
    const destination = req.session.returnTo || portalForRole(user.role);
    delete req.session.returnTo;
    res.redirect(destination);
  } catch (error) {
    const message = error instanceof ApiError ? error.message : 'Login failed';
    res.status(error instanceof ApiError ? error.status : 500).render('login', {
      title: 'Sign in',
      error: message,
      email,
      apiUrl: config.apiUrl,
      useFixtures: config.useFixtures,
    });
  }
});

router.post('/logout', (req, res) => {
  req.session.destroy(() => {
    res.redirect('/login');
  });
});

export default router;
