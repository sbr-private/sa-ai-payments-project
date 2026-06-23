package com.payments.ledger.adapter.mongo;

import com.payments.ledger.domain.SettlementAccounts;
import com.payments.ledger.domain.model.Account;
import com.payments.ledger.domain.model.AccountStatus;
import com.payments.ledger.domain.model.CreditDebitIndicator;
import com.payments.ledger.domain.model.IdempotencyFingerprint;
import com.payments.ledger.domain.model.Money;
import com.payments.ledger.domain.model.Party;
import com.payments.ledger.domain.model.PaymentTransaction;
import com.payments.ledger.domain.model.StatementEntry;
import com.payments.ledger.domain.model.StatementPage;
import com.payments.ledger.domain.model.StatusReason;
import com.payments.ledger.domain.model.TransactionStatus;
import com.payments.ledger.domain.model.TransferCommand;
import com.payments.ledger.domain.model.TransferOutcome;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.bson.Document;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

final class MongoSettlementEngine {

  private static final long SETTLEMENT_SEED_BALANCE_MINOR = 10_000_000_00L;

  private final MongoTemplate mongoTemplate;

  MongoSettlementEngine(MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }

  Optional<PaymentTransaction> findByEndToEndId(String endToEndId) {
    Query query =
        new Query(Criteria.where("pmtId.endToEndId").is(endToEndId));
    Document document =
        mongoTemplate.findOne(query, Document.class, MongoCollections.PAYMENT_TRANSACTIONS);
    if (document == null) {
      return Optional.empty();
    }
    return Optional.of(MongoPaymentTransactionMapper.toDomain(document));
  }

  TransferOutcome settleTransfer(TransferCommand command) {
    IdempotencyFingerprint fingerprint = IdempotencyFingerprint.from(command);

    Optional<PaymentTransaction> existing = findByEndToEndId(command.getEndToEndId());
    if (existing.isPresent()) {
      Document existingDoc =
          mongoTemplate.findOne(
              new Query(Criteria.where("pmtId.endToEndId").is(command.getEndToEndId())),
              Document.class,
              MongoCollections.PAYMENT_TRANSACTIONS);
      IdempotencyFingerprint stored =
          MongoPaymentTransactionMapper.fingerprintFromDocument(existingDoc);
      if (fingerprint.matches(stored)) {
        return TransferOutcome.replay(existing.get());
      }
      return TransferOutcome.conflict(existing.get());
    }

    if (command.getInstructedAmount().getValueMinor() <= 0) {
      return rejectTransfer(command, List.of(new StatusReason("AM12", List.of())));
    }

    if (command.getDebtorAccountId().equals(command.getCreditorAccountId())) {
      return rejectTransfer(command, List.of(new StatusReason("AG01", List.of())));
    }

    Optional<Account> debtorOpt = findAccount(command.getDebtorAccountId());
    Optional<Account> creditorOpt = findAccount(command.getCreditorAccountId());

    if (debtorOpt.isEmpty() || creditorOpt.isEmpty()) {
      return rejectTransfer(command, List.of(new StatusReason("BE01", List.of())));
    }

    Account debtor = debtorOpt.get();
    Account creditor = creditorOpt.get();

    if (debtor.getStatus() == AccountStatus.closed || creditor.getStatus() == AccountStatus.closed) {
      return rejectTransfer(command, List.of(new StatusReason("AC04", List.of())));
    }

    if (!debtor.getCcy().equals(command.getInstructedAmount().getCcy())
        || !creditor.getCcy().equals(command.getInstructedAmount().getCcy())
        || !debtor.getCcy().equals(creditor.getCcy())) {
      return rejectTransfer(command, List.of(new StatusReason("CURR", List.of())));
    }

    long amountMinor = command.getInstructedAmount().getValueMinor();
    if (debtor.getBalance().getValueMinor() < amountMinor) {
      return rejectTransfer(
          command,
          List.of(
              new StatusReason("AM04", List.of("Insufficient funds on debtor account"))));
    }

    return executeSettlement(command, debtor, creditor, fingerprint);
  }

  void creditAccount(UUID accountId, Money amount, String endToEndId) {
    ensureSettlementAccount();
    TransferCommand command =
        new TransferCommand(
            endToEndId,
            Optional.empty(),
            SettlementAccounts.SETTLEMENT_ACCOUNT_ID,
            accountId,
            amount);
    TransferOutcome outcome = settleTransfer(command);
    if (outcome.isConflict()) {
      throw new IllegalStateException(
          "Duplicate endToEndId with different credit parameters: " + endToEndId);
    }
    if (outcome.getTransaction().getStatus() == TransactionStatus.RJCT) {
      throw new IllegalStateException("Credit rejected for account " + accountId);
    }
  }

  void closeAccount(UUID accountId) {
    Account account =
        findAccount(accountId)
            .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));

    if (account.getBalance().getValueMinor() != 0) {
      throw new IllegalArgumentException("Account balance must be zero before closing");
    }

    if (account.getStatus() == AccountStatus.closed) {
      return;
    }

    Query query =
        new Query(
            Criteria.where("_id")
                .is(accountId.toString())
                .and("status")
                .is(AccountStatus.active.name()));
    Update update = new Update().set("status", AccountStatus.closed.name());
    Document updated =
        mongoTemplate.findAndModify(
            query,
            update,
            FindAndModifyOptions.options().returnNew(true),
            Document.class,
            MongoCollections.ACCOUNTS);

    if (updated == null) {
      throw new IllegalArgumentException("Account could not be closed: " + accountId);
    }
  }

  StatementPage findStatementEntries(UUID accountId, int limit, Optional<String> cursor) {
    if (findAccount(accountId).isEmpty()) {
      return new StatementPage(List.of(), Optional.empty(), false);
    }

    int pageSize = Math.max(1, Math.min(limit, 100));
    Query query = new Query(Criteria.where("acctId").is(accountId.toString()));

    cursor.ifPresent(
        value -> {
          String[] parts = value.split("\\|", 2);
          if (parts.length == 2) {
            Date cursorTime = Date.from(Instant.ofEpochMilli(Long.parseLong(parts[0])));
            String cursorId = parts[1];
            query.addCriteria(
                new Criteria()
                    .orOperator(
                        Criteria.where("creDtTm").lt(cursorTime),
                        new Criteria()
                            .andOperator(
                                Criteria.where("creDtTm").is(cursorTime),
                                Criteria.where("_id").lt(cursorId))));
          }
        });

    query.with(Sort.by(Sort.Order.desc("creDtTm"), Sort.Order.desc("_id")));
    query.limit(pageSize + 1);

    List<Document> documents =
        mongoTemplate.find(query, Document.class, MongoCollections.STATEMENT_ENTRIES);

    boolean hasMore = documents.size() > pageSize;
    List<Document> page = hasMore ? documents.subList(0, pageSize) : documents;

    List<StatementEntry> entries =
        page.stream().map(MongoStatementEntryMapper::toDomain).toList();

    Optional<String> nextCursor = Optional.empty();
    if (hasMore && !page.isEmpty()) {
      Document last = page.get(page.size() - 1);
      Instant lastTime = last.get("creDtTm", java.util.Date.class).toInstant();
      nextCursor = Optional.of(lastTime.toEpochMilli() + "|" + last.getString("_id"));
    }

    return new StatementPage(entries, nextCursor, hasMore);
  }

  private TransferOutcome executeSettlement(
      TransferCommand command,
      Account debtor,
      Account creditor,
      IdempotencyFingerprint fingerprint) {
    long amountMinor = command.getInstructedAmount().getValueMinor();
    String ccy = command.getInstructedAmount().getCcy();

    List<UUID> lockOrder = new ArrayList<>(List.of(debtor.getId(), creditor.getId()));
    lockOrder.sort(Comparator.naturalOrder());

    Document firstUpdated = null;
    Document secondUpdated = null;
    UUID firstId = lockOrder.get(0);
    UUID secondId = lockOrder.get(1);
    boolean firstIsDebtor = firstId.equals(debtor.getId());

    try {
      firstUpdated = debitOrCredit(firstId, firstIsDebtor, amountMinor, ccy);
      if (firstUpdated == null) {
        return rejectTransfer(
            command,
            List.of(
                new StatusReason("AM04", List.of("Insufficient funds on debtor account"))));
      }

      secondUpdated = debitOrCredit(secondId, !firstIsDebtor, amountMinor, ccy);
      if (secondUpdated == null) {
        rollbackBalance(firstId, firstIsDebtor, amountMinor);
        return rejectTransfer(
            command,
            List.of(
                new StatusReason("AM04", List.of("Insufficient funds on debtor account"))));
      }

      Instant now = Instant.now();
      UUID txId = UUID.randomUUID();
      LocalDate bookingDate = LocalDate.ofInstant(now, ZoneOffset.UTC);

      long debtorBalanceMinor =
          MongoAccountMapper.toDomain(
                  firstIsDebtor ? firstUpdated : secondUpdated)
              .getBalance()
              .getValueMinor();
      long creditorBalanceMinor =
          MongoAccountMapper.toDomain(
                  firstIsDebtor ? secondUpdated : firstUpdated)
              .getBalance()
              .getValueMinor();

      PaymentTransaction transaction =
          new PaymentTransaction(
              txId,
              command.getEndToEndId(),
              command.getInstrId(),
              command.getDebtorAccountId(),
              command.getCreditorAccountId(),
              command.getInstructedAmount(),
              TransactionStatus.ACSC,
              List.of(),
              now);

      try {
        mongoTemplate.insert(
            MongoPaymentTransactionMapper.toDocument(transaction, fingerprint),
            MongoCollections.PAYMENT_TRANSACTIONS);
      } catch (DuplicateKeyException ex) {
        rollbackBalance(debtor.getId(), true, amountMinor);
        rollbackBalance(creditor.getId(), false, amountMinor);
        return resolveIdempotencyRace(command);
      }

      StatementEntry debtorEntry =
          new StatementEntry(
              UUID.randomUUID(),
              debtor.getId(),
              txId,
              command.getEndToEndId(),
              command.getInstructedAmount(),
              CreditDebitIndicator.DBIT,
              new Money(debtorBalanceMinor, ccy),
              bookingDate,
              now);

      StatementEntry creditorEntry =
          new StatementEntry(
              UUID.randomUUID(),
              creditor.getId(),
              txId,
              command.getEndToEndId(),
              command.getInstructedAmount(),
              CreditDebitIndicator.CRDT,
              new Money(creditorBalanceMinor, ccy),
              bookingDate,
              now);

      mongoTemplate.insert(
          MongoStatementEntryMapper.toDocument(debtorEntry),
          MongoCollections.STATEMENT_ENTRIES);
      mongoTemplate.insert(
          MongoStatementEntryMapper.toDocument(creditorEntry),
          MongoCollections.STATEMENT_ENTRIES);

      return TransferOutcome.created(transaction);
    } catch (RuntimeException ex) {
      if (firstUpdated != null && secondUpdated == null) {
        rollbackBalance(firstId, firstIsDebtor, amountMinor);
      }
      throw ex;
    }
  }

  private Document debitOrCredit(UUID accountId, boolean debit, long amountMinor, String ccy) {
    Query query =
        new Query(
            Criteria.where("_id")
                .is(accountId.toString())
                .and("status")
                .is(AccountStatus.active.name())
                .and("bal.ccy")
                .is(ccy));

    if (debit) {
      query.addCriteria(Criteria.where("bal.valueMinor").gte(amountMinor));
    }

    long delta = debit ? -amountMinor : amountMinor;
    Update update = new Update().inc("bal.valueMinor", delta);

    return mongoTemplate.findAndModify(
        query,
        update,
        FindAndModifyOptions.options().returnNew(true),
        Document.class,
        MongoCollections.ACCOUNTS);
  }

  private void rollbackBalance(UUID accountId, boolean wasDebit, long amountMinor) {
    long delta = wasDebit ? amountMinor : -amountMinor;
    mongoTemplate.updateFirst(
        new Query(Criteria.where("_id").is(accountId.toString())),
        new Update().inc("bal.valueMinor", delta),
        MongoCollections.ACCOUNTS);
  }

  private TransferOutcome rejectTransfer(
      TransferCommand command, List<StatusReason> reasons) {
    Instant now = Instant.now();
    PaymentTransaction transaction =
        new PaymentTransaction(
            UUID.randomUUID(),
            command.getEndToEndId(),
            command.getInstrId(),
            command.getDebtorAccountId(),
            command.getCreditorAccountId(),
            command.getInstructedAmount(),
            TransactionStatus.RJCT,
            reasons,
            now);

    try {
      mongoTemplate.insert(
          MongoPaymentTransactionMapper.toDocument(
              transaction, IdempotencyFingerprint.from(command)),
          MongoCollections.PAYMENT_TRANSACTIONS);
      return TransferOutcome.created(transaction);
    } catch (DuplicateKeyException ex) {
      return resolveIdempotencyRace(command);
    }
  }

  private TransferOutcome resolveIdempotencyRace(TransferCommand command) {
    PaymentTransaction existing =
        findByEndToEndId(command.getEndToEndId())
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Duplicate endToEndId insert without existing row: "
                            + command.getEndToEndId()));

    Document existingDoc =
        mongoTemplate.findOne(
            new Query(Criteria.where("pmtId.endToEndId").is(command.getEndToEndId())),
            Document.class,
            MongoCollections.PAYMENT_TRANSACTIONS);
    IdempotencyFingerprint stored =
        MongoPaymentTransactionMapper.fingerprintFromDocument(existingDoc);
    IdempotencyFingerprint requested = IdempotencyFingerprint.from(command);

    if (requested.matches(stored)) {
      return TransferOutcome.replay(existing);
    }
    return TransferOutcome.conflict(existing);
  }

  private void ensureSettlementAccount() {
    UUID settlementId = SettlementAccounts.SETTLEMENT_ACCOUNT_ID;
    if (findAccount(settlementId).isPresent()) {
      return;
    }

    Account settlement =
        new Account(
            settlementId,
            new Party("Payments Co Settlement", Optional.empty()),
            "USD",
            new Money(SETTLEMENT_SEED_BALANCE_MINOR, "USD"),
            AccountStatus.active,
            Instant.parse("2026-06-23T11:00:00Z"));

    mongoTemplate.insert(MongoAccountMapper.toDocument(settlement), MongoCollections.ACCOUNTS);
  }

  private Optional<Account> findAccount(UUID id) {
    Document document =
        mongoTemplate.findById(id.toString(), Document.class, MongoCollections.ACCOUNTS);
    if (document == null) {
      return Optional.empty();
    }
    return Optional.of(MongoAccountMapper.toDomain(document));
  }
}
