package com.payments.ledger.domain.model;

import java.util.Objects;
import java.util.Optional;

public final class Party {

  private final String name;
  private final Optional<String> externalId;

  public Party(String name, Optional<String> externalId) {
    this.name = Objects.requireNonNull(name, "name");
    this.externalId = Objects.requireNonNull(externalId, "externalId");
  }

  public String getName() {
    return name;
  }

  public Optional<String> getExternalId() {
    return externalId;
  }
}
