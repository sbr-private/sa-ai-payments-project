package com.payments.ledger.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.payments.ledger.domain.model.AccountStatus;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccountResponse {

  private final String id;
  private final RegisterAccountRequest.PartyIdentificationDto owner;
  private final String ccy;
  private final CurrencyAndAmountDto bal;
  private final AccountStatus status;
  private final String creDtTm;

  public AccountResponse(
      String id,
      RegisterAccountRequest.PartyIdentificationDto owner,
      String ccy,
      CurrencyAndAmountDto bal,
      AccountStatus status,
      String creDtTm) {
    this.id = id;
    this.owner = owner;
    this.ccy = ccy;
    this.bal = bal;
    this.status = status;
    this.creDtTm = creDtTm;
  }

  public String getId() {
    return id;
  }

  public RegisterAccountRequest.PartyIdentificationDto getOwner() {
    return owner;
  }

  public String getCcy() {
    return ccy;
  }

  public CurrencyAndAmountDto getBal() {
    return bal;
  }

  public AccountStatus getStatus() {
    return status;
  }

  public String getCreDtTm() {
    return creDtTm;
  }
}
