package com.payments.ledger.domain.model;

import java.util.Objects;

/** Amount in ISO 4217 minor units plus currency code. */
public final class Money {

  private final long valueMinor;
  private final String ccy;

  public Money(long valueMinor, String ccy) {
    if (valueMinor < 0) {
      throw new IllegalArgumentException("valueMinor must be non-negative");
    }
    this.valueMinor = valueMinor;
    this.ccy = Objects.requireNonNull(ccy, "ccy");
  }

  public long getValueMinor() {
    return valueMinor;
  }

  public String getCcy() {
    return ccy;
  }
}
