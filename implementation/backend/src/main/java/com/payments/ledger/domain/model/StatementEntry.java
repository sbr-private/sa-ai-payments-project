package com.payments.ledger.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public final class StatementEntry {

  private final UUID entryRef;
  private final UUID accountId;
  private final UUID txId;
  private final String endToEndId;
  private final Money amount;
  private final CreditDebitIndicator creditDebitIndicator;
  private final Money balanceAfter;
  private final LocalDate bookingDate;
  private final Instant createdAt;

  public StatementEntry(
      UUID entryRef,
      UUID accountId,
      UUID txId,
      String endToEndId,
      Money amount,
      CreditDebitIndicator creditDebitIndicator,
      Money balanceAfter,
      LocalDate bookingDate,
      Instant createdAt) {
    this.entryRef = Objects.requireNonNull(entryRef, "entryRef");
    this.accountId = Objects.requireNonNull(accountId, "accountId");
    this.txId = Objects.requireNonNull(txId, "txId");
    this.endToEndId = Objects.requireNonNull(endToEndId, "endToEndId");
    this.amount = Objects.requireNonNull(amount, "amount");
    this.creditDebitIndicator =
        Objects.requireNonNull(creditDebitIndicator, "creditDebitIndicator");
    this.balanceAfter = Objects.requireNonNull(balanceAfter, "balanceAfter");
    this.bookingDate = Objects.requireNonNull(bookingDate, "bookingDate");
    this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
  }

  public UUID getEntryRef() {
    return entryRef;
  }

  public UUID getAccountId() {
    return accountId;
  }

  public UUID getTxId() {
    return txId;
  }

  public String getEndToEndId() {
    return endToEndId;
  }

  public Money getAmount() {
    return amount;
  }

  public CreditDebitIndicator getCreditDebitIndicator() {
    return creditDebitIndicator;
  }

  public Money getBalanceAfter() {
    return balanceAfter;
  }

  public LocalDate getBookingDate() {
    return bookingDate;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
