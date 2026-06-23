import { config } from '../config.js';

export function debtorAccountForE2e(endToEndId) {
  return config.seed.debtorByE2e[endToEndId] ?? config.seed.acmeAccountId;
}

export function extractTxStatus(pain002) {
  return pain002?.orgnlPmtInfAndSts?.[0]?.txInfAndSts?.[0] ?? null;
}

export function entryEndToEndId(entry) {
  return entry.ntryDtls?.[0]?.txDtls?.[0]?.refs?.endToEndId ?? null;
}
