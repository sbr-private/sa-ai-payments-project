package com.payments.ledger.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class Account {

  private final UUID id;
  private final Party owner;
  private final String ccy;
  private final Money balance;
  private final AccountStatus status;
  private final Instant createdAt;

  public Account(
      UUID id,
      Party owner,
      String ccy,
      Money balance,
      AccountStatus status,
      Instant createdAt) {
    this.id = Objects.requireNonNull(id, "id");
    this.owner = Objects.requireNonNull(owner, "owner");
    this.ccy = Objects.requireNonNull(ccy, "ccy");
    this.balance = Objects.requireNonNull(balance, "balance");
    this.status = Objects.requireNonNull(status, "status");
    this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
  }

  public UUID getId() {
    return id;
  }

  public Party getOwner() {
    return owner;
  }

  public String getCcy() {
    return ccy;
  }

  public Money getBalance() {
    return balance;
  }

  public AccountStatus getStatus() {
    return status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
