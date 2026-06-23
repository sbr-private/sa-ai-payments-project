package com.payments.ledger.api.mapper;

import com.payments.ledger.api.dto.payment.CustomerPaymentStatusReportResponse;
import com.payments.ledger.domain.model.PaymentTransaction;
import com.payments.ledger.domain.model.StatusReason;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PaymentStatusMapper {

  public CustomerPaymentStatusReportResponse.TransactionStatusInfoDto toStatusInfo(
      PaymentTransaction transaction) {
    List<CustomerPaymentStatusReportResponse.StatusReasonInformationDto> reasons =
        transaction.getStatusReasons().stream().map(this::toReasonDto).toList();

    List<CustomerPaymentStatusReportResponse.StatusReasonInformationDto> reasonList =
        reasons.isEmpty() ? null : reasons;

    return new CustomerPaymentStatusReportResponse.TransactionStatusInfoDto(
        transaction.getEndToEndId(), transaction.getStatus(), reasonList);
  }

  private CustomerPaymentStatusReportResponse.StatusReasonInformationDto toReasonDto(
      StatusReason reason) {
    return new CustomerPaymentStatusReportResponse.StatusReasonInformationDto(
        new CustomerPaymentStatusReportResponse.ReasonCodeDto(reason.getCode()),
        reason.getAdditionalInfo().isEmpty() ? null : reason.getAdditionalInfo());
  }
}
