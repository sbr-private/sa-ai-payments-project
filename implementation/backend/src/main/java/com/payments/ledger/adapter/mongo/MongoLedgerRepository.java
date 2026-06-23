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
  private final MongoSettlementEngine settlementEngine;

  public MongoLedgerRepository(MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
    this.settlementEngine = new MongoSettlementEngine(mongoTemplate);
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
    Document document =
        mongoTemplate.findById(id.toString(), Document.class, MongoCollections.ACCOUNTS);
    if (document == null) {
      return Optional.empty();
    }
    return Optional.of(MongoAccountMapper.toDomain(document));
  }

  @Override
  public Account insertAccount(Account account) {
    mongoTemplate.insert(MongoAccountMapper.toDocument(account), MongoCollections.ACCOUNTS);
    return account;
  }

  @Override
  public Optional<PaymentTransaction> findByEndToEndId(String endToEndId) {
    return settlementEngine.findByEndToEndId(endToEndId);
  }

  @Override
  public StatementPage findStatementEntries(UUID accountId, int limit, Optional<String> cursor) {
    return settlementEngine.findStatementEntries(accountId, limit, cursor);
  }

  @Override
  public TransferOutcome settleTransfer(TransferCommand command) {
    return settlementEngine.settleTransfer(command);
  }

  @Override
  public void creditAccount(UUID accountId, Money amount, String endToEndId) {
    settlementEngine.creditAccount(accountId, amount, endToEndId);
  }

  @Override
  public void closeAccount(UUID accountId) {
    settlementEngine.closeAccount(accountId);
  }
}
