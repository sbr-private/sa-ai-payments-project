package com.payments.ledger.api;

import com.payments.ledger.api.auth.AuthContext;
import com.payments.ledger.api.dto.payment.CustomerCreditTransferInitiationRequest;
import com.payments.ledger.api.dto.payment.CustomerPaymentStatusReportResponse;
import com.payments.ledger.domain.service.PaymentInitiationResult;
import com.payments.ledger.domain.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PaymentInitiationController {

  private final PaymentService paymentService;

  public PaymentInitiationController(PaymentService paymentService) {
    this.paymentService = paymentService;
  }

  @PostMapping("/payment-initiations")
  public ResponseEntity<CustomerPaymentStatusReportResponse> initiate(
      @Valid @RequestBody CustomerCreditTransferInitiationRequest request,
      HttpServletRequest httpRequest) {
    PaymentInitiationResult result =
        paymentService.initiate(request, AuthContext.requireUser(httpRequest));
    return ResponseEntity.status(result.getHttpStatus()).body(result.getReport());
  }
}
