package com.payments.ledger.domain.service;

import com.payments.ledger.api.dto.payment.CustomerCreditTransferInitiationRequest;
import com.payments.ledger.api.dto.payment.CustomerPaymentStatusReportResponse;
import com.payments.ledger.domain.auth.DemoUser;
import com.payments.ledger.domain.auth.PaymentAccess;
import com.payments.ledger.domain.model.Money;
import com.payments.ledger.domain.model.PaymentTransaction;
import com.payments.ledger.domain.model.StatusReason;
import com.payments.ledger.domain.model.TransactionStatus;
import com.payments.ledger.domain.model.TransferCommand;
import com.payments.ledger.domain.model.TransferOutcome;
import com.payments.ledger.domain.money.MoneyConverter;
import com.payments.ledger.repository.LedgerRepository;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {

  private static final DateTimeFormatter CRE_DT_TM_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(java.time.ZoneOffset.UTC);

  private final LedgerRepository ledgerRepository;

  public PaymentService(LedgerRepository ledgerRepository) {
    this.ledgerRepository = ledgerRepository;
  }

  public PaymentInitiationResult initiate(
      CustomerCreditTransferInitiationRequest request, DemoUser requester) {
    List<CustomerPaymentStatusReportResponse.TransactionStatusInfoDto> txStatuses =
        new ArrayList<>();
    List<String> pmtInfIds = new ArrayList<>();

    TransferOutcome lastOutcome = null;

    for (CustomerCreditTransferInitiationRequest.PaymentInstructionDto pmtInf :
        request.getPmtInf()) {
      pmtInfIds.add(pmtInf.getPmtInfId());

      for (CustomerCreditTransferInitiationRequest.CreditTransferTransactionDto tx :
          pmtInf.getCdtTrfTxInf()) {
        UUID debtorId = UUID.fromString(pmtInf.getDbtrAcct().getId().getOthr().getId());
        PaymentAccess.requireCanInitiate(requester, debtorId);

        UUID creditorId = UUID.fromString(tx.getCdtrAcct().getId().getOthr().getId());
        Optional<String> instrId = Optional.ofNullable(tx.getPmtId().getInstrId());
        String endToEndId = tx.getPmtId().getEndToEndId();

        Money amount = parseAmount(tx.getAmt().getInstdAmt());
        TransferCommand command =
            new TransferCommand(endToEndId, instrId, debtorId, creditorId, amount);

        lastOutcome = ledgerRepository.settleTransfer(command);
        PaymentTransaction transaction = lastOutcome.getTransaction();

        txStatuses.add(toStatusInfo(transaction));

        if (lastOutcome.isConflict()) {
          txStatuses.set(
              txStatuses.size() - 1,
              new CustomerPaymentStatusReportResponse.TransactionStatusInfoDto(
                  endToEndId,
                  TransactionStatus.RJCT,
                  List.of(
                      new CustomerPaymentStatusReportResponse.StatusReasonInformationDto(
                          new CustomerPaymentStatusReportResponse.ReasonCodeDto("DU04"),
                          null))));
          return new PaymentInitiationResult(
              HttpStatus.CONFLICT,
              buildReport(request, pmtInfIds, txStatuses, transaction));
        }
      }
    }

    if (lastOutcome == null) {
      throw new IllegalArgumentException("No payment transactions in request");
    }

    HttpStatus status = lastOutcome.isReplay() ? HttpStatus.OK : HttpStatus.CREATED;
    return new PaymentInitiationResult(
        status,
        buildReport(
            request,
            pmtInfIds,
            txStatuses,
            lastOutcome.getTransaction()));
  }

  private Money parseAmount(com.payments.ledger.api.dto.CurrencyAndAmountDto dto) {
    try {
      long minor = MoneyConverter.toMinorUnits(dto.getValue(), dto.getCcy());
      return new Money(minor, dto.getCcy());
    } catch (IllegalArgumentException | ArithmeticException ex) {
      return new Money(0, dto.getCcy());
    }
  }

  private CustomerPaymentStatusReportResponse.TransactionStatusInfoDto toStatusInfo(
      PaymentTransaction transaction) {
    List<CustomerPaymentStatusReportResponse.StatusReasonInformationDto> reasons =
        transaction.getStatusReasons().stream()
            .map(this::toReasonDto)
            .toList();

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

  private CustomerPaymentStatusReportResponse buildReport(
      CustomerCreditTransferInitiationRequest request,
      List<String> pmtInfIds,
      List<CustomerPaymentStatusReportResponse.TransactionStatusInfoDto> txStatuses,
      PaymentTransaction lastTransaction) {
    TransactionStatus groupStatus = resolveGroupStatus(txStatuses);

    List<CustomerPaymentStatusReportResponse.OriginalPaymentInfoDto> pmtInfStatuses =
        new ArrayList<>();
    int txIndex = 0;
    for (int i = 0; i < request.getPmtInf().size(); i++) {
      int txCount = request.getPmtInf().get(i).getCdtTrfTxInf().size();
      List<CustomerPaymentStatusReportResponse.TransactionStatusInfoDto> slice =
          txStatuses.subList(txIndex, txIndex + txCount);
      pmtInfStatuses.add(
          new CustomerPaymentStatusReportResponse.OriginalPaymentInfoDto(
              pmtInfIds.get(i), slice));
      txIndex += txCount;
    }

    return new CustomerPaymentStatusReportResponse(
        new CustomerPaymentStatusReportResponse.StatusGroupHeaderDto(
            "STS-" + Instant.now().toEpochMilli(),
            CRE_DT_TM_FORMATTER.format(Instant.now())),
        new CustomerPaymentStatusReportResponse.OriginalGroupInfoDto(
            request.getGrpHdr().getMsgId(), "pain.001.001.09", groupStatus.name()),
        pmtInfStatuses);
  }

  private TransactionStatus resolveGroupStatus(
      List<CustomerPaymentStatusReportResponse.TransactionStatusInfoDto> txStatuses) {
    boolean anyReject =
        txStatuses.stream().anyMatch(tx -> tx.getTxSts() == TransactionStatus.RJCT);
    if (anyReject) {
      return TransactionStatus.RJCT;
    }
    return TransactionStatus.ACSC;
  }
}
