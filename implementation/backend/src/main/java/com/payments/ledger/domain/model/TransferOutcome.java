package com.payments.ledger.domain.model;

import java.util.Objects;
import java.util.Optional;

public final class TransferOutcome {

  public enum Kind {
    CREATED,
    REPLAY,
    CONFLICT
  }

  private final Kind kind;
  private final PaymentTransaction transaction;

  private TransferOutcome(Kind kind, PaymentTransaction transaction) {
    this.kind = kind;
    this.transaction = Objects.requireNonNull(transaction, "transaction");
  }

  public static TransferOutcome created(PaymentTransaction transaction) {
    return new TransferOutcome(Kind.CREATED, transaction);
  }

  public static TransferOutcome replay(PaymentTransaction transaction) {
    return new TransferOutcome(Kind.REPLAY, transaction);
  }

  public static TransferOutcome conflict(PaymentTransaction transaction) {
    return new TransferOutcome(Kind.CONFLICT, transaction);
  }

  public Kind getKind() {
    return kind;
  }

  public PaymentTransaction getTransaction() {
    return transaction;
  }

  public boolean isReplay() {
    return kind == Kind.REPLAY;
  }

  public boolean isConflict() {
    return kind == Kind.CONFLICT;
  }
}
