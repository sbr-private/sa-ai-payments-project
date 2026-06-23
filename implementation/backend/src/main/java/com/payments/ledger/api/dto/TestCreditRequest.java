package com.payments.ledger.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class TestCreditRequest {

  @NotNull @Valid private CurrencyAndAmountDto amount;

  @NotBlank private String endToEndId;

  public CurrencyAndAmountDto getAmount() {
    return amount;
  }

  public void setAmount(CurrencyAndAmountDto amount) {
    this.amount = amount;
  }

  public String getEndToEndId() {
    return endToEndId;
  }

  public void setEndToEndId(String endToEndId) {
    this.endToEndId = endToEndId;
  }
}
