package com.payments.ledger.domain.model;

import java.util.Objects;
import java.util.UUID;

public final class IdempotencyFingerprint {

  private final UUID debtorAccountId;
  private final UUID creditorAccountId;
  private final Money instructedAmount;

  public IdempotencyFingerprint(
      UUID debtorAccountId, UUID creditorAccountId, Money instructedAmount) {
    this.debtorAccountId = Objects.requireNonNull(debtorAccountId, "debtorAccountId");
    this.creditorAccountId = Objects.requireNonNull(creditorAccountId, "creditorAccountId");
    this.instructedAmount = Objects.requireNonNull(instructedAmount, "instructedAmount");
  }

  public static IdempotencyFingerprint from(TransferCommand command) {
    return new IdempotencyFingerprint(
        command.getDebtorAccountId(),
        command.getCreditorAccountId(),
        command.getInstructedAmount());
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

  public boolean matches(IdempotencyFingerprint other) {
    return debtorAccountId.equals(other.debtorAccountId)
        && creditorAccountId.equals(other.creditorAccountId)
        && instructedAmount.getValueMinor() == other.instructedAmount.getValueMinor()
        && instructedAmount.getCcy().equals(other.instructedAmount.getCcy());
  }
}
