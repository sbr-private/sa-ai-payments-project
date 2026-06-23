package com.payments.ledger.domain.model;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class StatementPage {

  private final List<StatementEntry> entries;
  private final Optional<String> nextCursor;
  private final boolean hasMore;

  public StatementPage(
      List<StatementEntry> entries, Optional<String> nextCursor, boolean hasMore) {
    this.entries = List.copyOf(entries);
    this.nextCursor = Objects.requireNonNull(nextCursor, "nextCursor");
    this.hasMore = hasMore;
  }

  public List<StatementEntry> getEntries() {
    return entries;
  }

  public Optional<String> getNextCursor() {
    return nextCursor;
  }

  public boolean isHasMore() {
    return hasMore;
  }
}
