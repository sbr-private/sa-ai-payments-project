package com.payments.ledger.domain.model;

import java.util.List;
import java.util.Objects;

public final class StatusReason {

  private final String code;
  private final List<String> additionalInfo;

  public StatusReason(String code, List<String> additionalInfo) {
    this.code = Objects.requireNonNull(code, "code");
    this.additionalInfo = List.copyOf(additionalInfo);
  }

  public String getCode() {
    return code;
  }

  public List<String> getAdditionalInfo() {
    return additionalInfo;
  }
}
