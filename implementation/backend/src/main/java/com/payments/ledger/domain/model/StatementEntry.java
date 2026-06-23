package com.payments.ledger.domain.model;

import java.util.UUID;

/** Placeholder for camt.053 statement row — fields added when OP-005 is implemented. */
public final class StatementEntry {

  private final UUID entryRef;

  public StatementEntry(UUID entryRef) {
    this.entryRef = entryRef;
  }

  public UUID getEntryRef() {
    return entryRef;
  }
}
