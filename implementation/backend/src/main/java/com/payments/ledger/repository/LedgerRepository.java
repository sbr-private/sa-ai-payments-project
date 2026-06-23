package com.payments.ledger.repository;

import com.payments.ledger.domain.model.Account;
import com.payments.ledger.domain.model.Money;
import com.payments.ledger.domain.model.PaymentTransaction;
import com.payments.ledger.domain.model.StatementPage;
import com.payments.ledger.domain.model.TransferCommand;
import com.payments.ledger.domain.model.TransferOutcome;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence port for the ledger. Domain and API layers depend on this interface only.
 *
 * <p>Settlement ({@link #settleTransfer}) must run atomically inside the adapter — Postgres uses
 * row locks and a JDBC transaction; Mongo uses a multi-document transaction. The same contract
 * applies to both backends.
 */
public interface LedgerRepository {

  /** Returns {@code true} when the configured database is reachable. */
  boolean isHealthy();

  Optional<Account> findAccountById(UUID id);

  Account insertAccount(Account account);

  Optional<PaymentTransaction> findByEndToEndId(String endToEndId);

  StatementPage findStatementEntries(UUID accountId, int limit, Optional<String> cursor);

  TransferOutcome settleTransfer(TransferCommand command);

  /** Test helper — credit an account (requires {@code ledger.test-helpers.enabled=true}). */
  void creditAccount(UUID accountId, Money amount, String endToEndId);

  /** Test helper — close an account (requires {@code ledger.test-helpers.enabled=true}). */
  void closeAccount(UUID accountId);
}
