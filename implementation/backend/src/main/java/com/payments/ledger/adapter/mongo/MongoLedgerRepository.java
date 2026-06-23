package com.payments.ledger.adapter.mongo;

import com.payments.ledger.domain.model.Account;
import com.payments.ledger.domain.model.Money;
import com.payments.ledger.domain.model.PaymentTransaction;
import com.payments.ledger.domain.model.StatementPage;
import com.payments.ledger.domain.model.TransferCommand;
import com.payments.ledger.domain.model.TransferOutcome;
import com.payments.ledger.repository.LedgerRepository;
import java.util.Optional;
import java.util.UUID;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;

public class MongoLedgerRepository implements LedgerRepository {

  private final MongoTemplate mongoTemplate;

  public MongoLedgerRepository(MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }

  @Override
  public boolean isHealthy() {
    try {
      mongoTemplate.getDb().runCommand(new Document("ping", 1));
      return true;
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
    return new UnsupportedOperationException("Mongo adapter operation not implemented yet");
  }
}
