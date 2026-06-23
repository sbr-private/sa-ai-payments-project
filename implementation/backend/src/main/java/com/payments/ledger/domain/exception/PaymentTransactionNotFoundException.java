package com.payments.ledger.domain.exception;

public class PaymentTransactionNotFoundException extends RuntimeException {

  private final String endToEndId;

  public PaymentTransactionNotFoundException(String endToEndId) {
    super("Payment transaction not found");
    this.endToEndId = endToEndId;
  }

  public String getEndToEndId() {
    return endToEndId;
  }
}
