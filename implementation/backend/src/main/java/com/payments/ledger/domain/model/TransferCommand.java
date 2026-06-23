package com.payments.ledger.domain.model;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Input for atomic settlement — implemented inside each adapter. */
public final class TransferCommand {

  private final String endToEndId;
  private final Optional<String> instrId;
  private final UUID debtorAccountId;
  private final UUID creditorAccountId;
  private final Money instructedAmount;

  public TransferCommand(
      String endToEndId,
      Optional<String> instrId,
      UUID debtorAccountId,
      UUID creditorAccountId,
      Money instructedAmount) {
    this.endToEndId = Objects.requireNonNull(endToEndId, "endToEndId");
    this.instrId = Objects.requireNonNull(instrId, "instrId");
    this.debtorAccountId = Objects.requireNonNull(debtorAccountId, "debtorAccountId");
    this.creditorAccountId = Objects.requireNonNull(creditorAccountId, "creditorAccountId");
    this.instructedAmount = Objects.requireNonNull(instructedAmount, "instructedAmount");
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
}
