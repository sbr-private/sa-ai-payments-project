import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));

export function notFoundHandler(req, res) {
  res.status(404).render('error', {
    title: 'Not found',
    message: `No page at ${req.path}`,
    hint: null,
    actionHref: '/',
    actionLabel: 'Home',
  });
}

export function errorHandler(err, req, res, _next) {
  console.error(err);

  const status = err.status || 500;
  const message = err.message || 'Something went wrong';

  if (req.path.startsWith('/api/')) {
    return res.status(status).json(err.body || { error: { code: 'INTERNAL_ERROR', message } });
  }

  res.status(status).render('error', {
    title: status >= 500 ? 'Server error' : 'Request failed',
    message,
    hint: err.body?.error?.message && err.body.error.message !== message ? err.body.error.message : null,
    actionHref: 'back',
    actionLabel: 'Go back',
  });
}

export const viewsPath = path.join(__dirname, '../views');
export const publicPath = path.join(__dirname, '../public');
