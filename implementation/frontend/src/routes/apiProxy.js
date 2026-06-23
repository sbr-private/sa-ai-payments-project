import { Router } from 'express';
import { createProxyMiddleware } from 'http-proxy-middleware';
import { config } from '../config.js';
import { requireAuth } from '../middleware/auth.js';

const router = Router();

router.use(requireAuth);

router.use(
  '/',
  createProxyMiddleware({
    target: config.apiUrl,
    changeOrigin: true,
    pathRewrite: { '^/api': '' },
    on: {
      proxyReq(proxyReq, req) {
        if (req.session?.user?.email) {
          proxyReq.setHeader('X-Demo-User', req.session.user.email);
        }
      },
      error(err, _req, res) {
        res.status(502).json({
          error: {
            code: 'UPSTREAM_ERROR',
            message: 'Ledger API is unreachable',
            details: { reason: err.message },
          },
        });
      },
    },
  }),
);

export default router;
