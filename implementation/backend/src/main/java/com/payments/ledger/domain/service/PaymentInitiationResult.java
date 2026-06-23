package com.payments.ledger.domain.service;

import com.payments.ledger.api.dto.payment.CustomerPaymentStatusReportResponse;
import org.springframework.http.HttpStatus;

public final class PaymentInitiationResult {

  private final HttpStatus httpStatus;
  private final CustomerPaymentStatusReportResponse report;

  public PaymentInitiationResult(
      HttpStatus httpStatus, CustomerPaymentStatusReportResponse report) {
    this.httpStatus = httpStatus;
    this.report = report;
  }

  public HttpStatus getHttpStatus() {
    return httpStatus;
  }

  public CustomerPaymentStatusReportResponse getReport() {
    return report;
  }
}
