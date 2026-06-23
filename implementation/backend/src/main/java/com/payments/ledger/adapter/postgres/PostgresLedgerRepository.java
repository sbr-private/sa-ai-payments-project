package com.payments.ledger.adapter.postgres;

import com.payments.ledger.domain.model.Account;
import com.payments.ledger.domain.model.Money;
import com.payments.ledger.domain.model.PaymentTransaction;
import com.payments.ledger.domain.model.StatementPage;
import com.payments.ledger.domain.model.TransferCommand;
import com.payments.ledger.domain.model.TransferOutcome;
import com.payments.ledger.repository.LedgerRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

public class PostgresLedgerRepository implements LedgerRepository {

  private final JdbcTemplate jdbcTemplate;

  public PostgresLedgerRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public boolean isHealthy() {
    try {
      Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
      return result != null && result == 1;
    } catch (RuntimeException ex) {
      return false;
    }
  }

  @Override
  public Optional<Account> findAccountById(UUID id) {
    throw notImplemented();
  }

  @Override
  public Account insertAccount(Account account) {
    throw notImplemented();
  }

  @Override
  public Optional<PaymentTransaction> findByEndToEndId(String endToEndId) {
    throw notImplemented();
  }

  @Override
  public StatementPage findStatementEntries(UUID accountId, int limit, Optional<String> cursor) {
    throw notImplemented();
  }

  @Override
  public TransferOutcome settleTransfer(TransferCommand command) {
    throw notImplemented();
  }

  @Override
  public void creditAccount(UUID accountId, Money amount, String endToEndId) {
    throw notImplemented();
  }

  @Override
  public void closeAccount(UUID accountId) {
    throw notImplemented();
  }

  private static UnsupportedOperationException notImplemented() {
    return new UnsupportedOperationException("Postgres adapter operation not implemented yet");
  }
}
