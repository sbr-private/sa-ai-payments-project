import express from 'express';
import session from 'express-session';
import path from 'node:path';
import { config } from './config.js';
import { attachUser } from './middleware/auth.js';
import { errorHandler, notFoundHandler, publicPath, viewsPath } from './middleware/errorHandler.js';
import authRoutes from './routes/auth.js';
import payerRoutes from './routes/payer.js';
import supportRoutes from './routes/support.js';
import apiProxyRoutes from './routes/apiProxy.js';
import docsRoutes from './routes/docs.js';

const app = express();

app.set('view engine', 'ejs');
app.set('views', viewsPath);

app.use(express.urlencoded({ extended: false }));
app.use(express.json());
app.use(express.static(publicPath));

app.use(
  session({
    secret: config.sessionSecret,
    resave: false,
    saveUninitialized: false,
    cookie: {
      httpOnly: true,
      maxAge: 8 * 60 * 60 * 1000,
    },
  }),
);

app.use(attachUser);

app.get('/', (req, res) => {
  if (!req.session?.user) {
    return res.redirect('/login');
  }
  const portal = req.session.user.role === 'support' ? '/support' : '/payer';
  res.redirect(portal);
});

app.use('/', authRoutes);
app.use('/payer', payerRoutes);
app.use('/support', supportRoutes);
app.use('/api', apiProxyRoutes);
app.use('/api-docs', docsRoutes);

app.use(notFoundHandler);
app.use(errorHandler);

app.listen(config.port, () => {
  console.log(`AmazingPayments UI  http://localhost:${config.port}`);
  console.log(`Ledger API: ${config.apiUrl}`);
  console.log(`API docs:   http://localhost:${config.port}/api-docs/ledger`);
  if (config.useFixtures) {
    console.log('Fixture mode enabled (USE_FIXTURES=true)');
  }
});
