package com.payments.ledger.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class PaymentTransaction {

  private final UUID txId;
  private final String endToEndId;
  private final Optional<String> instrId;
  private final UUID debtorAccountId;
  private final UUID creditorAccountId;
  private final Money instructedAmount;
  private final TransactionStatus status;
  private final List<StatusReason> statusReasons;
  private final Instant createdAt;

  public PaymentTransaction(
      UUID txId,
      String endToEndId,
      Optional<String> instrId,
      UUID debtorAccountId,
      UUID creditorAccountId,
      Money instructedAmount,
      TransactionStatus status,
      List<StatusReason> statusReasons,
      Instant createdAt) {
    this.txId = Objects.requireNonNull(txId, "txId");
    this.endToEndId = Objects.requireNonNull(endToEndId, "endToEndId");
    this.instrId = Objects.requireNonNull(instrId, "instrId");
    this.debtorAccountId = Objects.requireNonNull(debtorAccountId, "debtorAccountId");
    this.creditorAccountId = Objects.requireNonNull(creditorAccountId, "creditorAccountId");
    this.instructedAmount = Objects.requireNonNull(instructedAmount, "instructedAmount");
    this.status = Objects.requireNonNull(status, "status");
    this.statusReasons = List.copyOf(statusReasons);
    this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
  }

  public UUID getTxId() {
    return txId;
  }

  public String getEndToEndId() {
    return endToEndId;
  }

  public Optional<String> getInstrId() {
    return instrId;
  }

  public UUID getDebtorAccountId() {
    return debtorAccountId;
  }

  public UUID getCreditorAccountId() {
    return creditorAccountId;
  }

  public Money getInstructedAmount() {
    return instructedAmount;
  }

  public TransactionStatus getStatus() {
    return status;
  }

  public List<StatusReason> getStatusReasons() {
    return statusReasons;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
