import { config } from '../config.js';
import {
  getFixtureAccount,
  getFixtureStatement,
  getFixtureTransaction,
  postFixturePayment,
} from './fixtures.js';

export class ApiError extends Error {
  constructor(status, body) {
    super(body?.error?.message || `API error ${status}`);
    this.status = status;
    this.body = body;
  }
}

async function request(method, path, { userEmail, body } = {}) {
  if (config.useFixtures) {
    return fixtureRequest(method, path, body);
  }

  const headers = { Accept: 'application/json' };
  if (body !== undefined) {
    headers['Content-Type'] = 'application/json';
  }
  if (userEmail) {
    headers['X-Demo-User'] = userEmail;
  }

  const response = await fetch(`${config.apiUrl}${path}`, {
    method,
    headers,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });

  const text = await response.text();
  const payload = text ? JSON.parse(text) : null;

  if (!response.ok) {
    throw new ApiError(response.status, payload);
  }

  return { status: response.status, body: payload };
}

async function fixtureRequest(method, path, body) {
  if (method === 'GET' && path === '/health') {
    return { status: 200, body: { status: 'ok' } };
  }

  const accountMatch = path.match(/^\/accounts\/([^/]+)$/);
  if (method === 'GET' && accountMatch) {
    const account = await getFixtureAccount(accountMatch[1]);
    if (!account) throw new ApiError(404, { error: { code: 'NOT_FOUND', message: 'Account not found' } });
    return { status: 200, body: account };
  }

  const statementMatch = path.match(/^\/accounts\/([^/]+)\/statements/);
  if (method === 'GET' && statementMatch) {
    const account = await getFixtureAccount(statementMatch[1]);
    if (!account) throw new ApiError(404, { error: { code: 'NOT_FOUND', message: 'Account not found' } });
    return { status: 200, body: await getFixtureStatement(statementMatch[1]) };
  }

  const txMatch = path.match(/^\/payment-initiations\/transactions\/(.+)$/);
  if (method === 'GET' && txMatch) {
    const tx = await getFixtureTransaction(decodeURIComponent(txMatch[1]));
    if (!tx) throw new ApiError(404, { error: { code: 'NOT_FOUND', message: 'Transaction not found' } });
    return { status: 200, body: tx };
  }

  if (method === 'POST' && path === '/payment-initiations') {
    try {
      return await postFixturePayment(body);
    } catch (error) {
      if (error.status) throw new ApiError(error.status, error.body);
      throw error;
    }
  }

  if (method === 'POST' && path === '/auth/login') {
    const { readFile } = await import('node:fs/promises');
    const userList = JSON.parse(await readFile(`${config.fixturesPath}/seed-users.json`, 'utf8'));
    const user = userList.find((u) => u.email === body.email && u.password === body.password);
    if (!user) throw new ApiError(401, { error: { code: 'INVALID_CREDENTIALS', message: 'Invalid credentials' } });
    const { password: _, ...safeUser } = user;
    return { status: 200, body: { user: safeUser } };
  }

  throw new ApiError(501, { error: { code: 'NOT_IMPLEMENTED', message: `Fixture not available for ${method} ${path}` } });
}

export async function login(email, password) {
  const { body } = await request('POST', '/auth/login', { body: { email, password } });
  return body.user;
}

export async function getHealth() {
  const { body } = await request('GET', '/health');
  return body;
}

export async function getAccount(userEmail, accountId) {
  const { body } = await request('GET', `/accounts/${accountId}`, { userEmail });
  return body;
}

export async function getStatement(userEmail, accountId, { limit = 20, cursor } = {}) {
  const params = new URLSearchParams({ limit: String(limit) });
  if (cursor) params.set('cursor', cursor);
  const { body } = await request('GET', `/accounts/${accountId}/statements?${params}`, { userEmail });
  return body;
}

export async function getTransactionStatus(userEmail, endToEndId) {
  const { body } = await request('GET', `/payment-initiations/transactions/${encodeURIComponent(endToEndId)}`, {
    userEmail,
  });
  return body;
}

export async function createPaymentInitiation(userEmail, initiation) {
  const { status, body } = await request('POST', '/payment-initiations', { userEmail, body: initiation });
  return { status, body };
}
