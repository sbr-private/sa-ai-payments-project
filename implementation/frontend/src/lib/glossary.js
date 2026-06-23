/** User-facing labels and ISO tooltip copy for the AmazingPayments UI. */

export const terms = {
  endToEndId: {
    label: 'Payment reference',
    technical: 'EndToEndId (ISO 20022)',
    definition: 'A unique reference you assign to a payment. The same reference always returns the same result if you submit again.',
  },
  pain001: {
    label: 'Payment request',
    technical: 'pain.001',
    definition: 'Customer Credit Transfer Initiation — the message that asks the bank to send money.',
  },
  pain002: {
    label: 'Payment status',
    technical: 'pain.002',
    definition: 'Customer Payment Status Report — tells you whether a payment was accepted, rejected, or is still processing.',
  },
  camt053: {
    label: 'Account statement',
    technical: 'camt.053',
    definition: 'Bank-to-Customer Statement — a list of credits and debits posted to your account.',
  },
  iso20022: {
    label: 'ISO 20022',
    technical: 'ISO 20022',
    definition: 'The international standard for payment messaging between banks and corporates.',
  },
  txStatus: {
    label: 'Payment status',
    technical: 'txSts',
    definition: 'Transaction status code from the payment status report.',
  },
  rejectionReason: {
    label: 'Rejection reason',
    technical: 'stsRsnInf',
    definition: 'Explains why a payment was rejected, including a standard reason code.',
  },
  reasonCode: {
    label: 'Reason code',
    technical: 'rsn.cd',
    definition: 'A standard code describing why a payment failed (e.g. insufficient funds).',
  },
  idempotency: {
    label: 'duplicate-safe reference',
    technical: 'Idempotency key',
    definition: 'Submitting the same payment reference twice will not create a second payment.',
  },
  creditTransfer: {
    label: 'Send money',
    technical: 'Credit transfer (pain.001)',
    definition: 'Initiate a payment from your account to a beneficiary.',
  },
  directionCrdt: {
    label: 'Money in',
    technical: 'CRDT',
    definition: 'Credit — funds received into the account.',
  },
  directionDbit: {
    label: 'Money out',
    technical: 'DBIT',
    definition: 'Debit — funds sent from the account.',
  },
};

export const txStatusLabels = {
  ACSC: { label: 'Settled', definition: 'Accepted Settlement Completed — the payment was successful.' },
  RJCT: { label: 'Rejected', definition: 'The payment was declined and no money moved.' },
  PDNG: { label: 'Pending', definition: 'The payment is still being processed.' },
};

export const reasonCodeLabels = {
  AM04: { label: 'Insufficient funds', definition: 'The account did not have enough balance for the instructed amount.' },
  AC04: { label: 'Account closed', definition: 'The debtor account is closed.' },
  AM12: { label: 'Invalid amount', definition: 'The payment amount is not allowed.' },
  CURR: { label: 'Currency mismatch', definition: 'The currencies on the accounts do not match.' },
  DU04: { label: 'Duplicate reference', definition: 'This payment reference was already used with different details.' },
  AG01: { label: 'Not permitted', definition: 'This type of transaction is not allowed on the account.' },
  BE01: { label: 'Unknown beneficiary', definition: 'The beneficiary account could not be identified.' },
};

export function formatTxStatus(code) {
  return txStatusLabels[code] ?? { label: code, definition: `Status code: ${code}` };
}

export function formatReasonCode(code) {
  return reasonCodeLabels[code] ?? { label: code, definition: `Reason code: ${code}` };
}
