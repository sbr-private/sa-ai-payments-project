package com.payments.ledger.api.dto;

public class CurrencyAndAmountDto {

  private final String value;
  private final String ccy;

  public CurrencyAndAmountDto(String value, String ccy) {
    this.value = value;
    this.ccy = ccy;
  }

  public String getValue() {
    return value;
  }

  public String getCcy() {
    return ccy;
  }
}
