package com.payments.ledger.api.dto.payment;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.payments.ledger.domain.model.TransactionStatus;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CustomerPaymentStatusReportResponse {

  private final StatusGroupHeaderDto grpHdr;
  private final OriginalGroupInfoDto orgnlGrpInfAndSts;
  private final List<OriginalPaymentInfoDto> orgnlPmtInfAndSts;

  public CustomerPaymentStatusReportResponse(
      StatusGroupHeaderDto grpHdr,
      OriginalGroupInfoDto orgnlGrpInfAndSts,
      List<OriginalPaymentInfoDto> orgnlPmtInfAndSts) {
    this.grpHdr = grpHdr;
    this.orgnlGrpInfAndSts = orgnlGrpInfAndSts;
    this.orgnlPmtInfAndSts = orgnlPmtInfAndSts;
  }

  public StatusGroupHeaderDto getGrpHdr() {
    return grpHdr;
  }

  public OriginalGroupInfoDto getOrgnlGrpInfAndSts() {
    return orgnlGrpInfAndSts;
  }

  public List<OriginalPaymentInfoDto> getOrgnlPmtInfAndSts() {
    return orgnlPmtInfAndSts;
  }

  public static class StatusGroupHeaderDto {

    private final String msgId;
    private final String creDtTm;

    public StatusGroupHeaderDto(String msgId, String creDtTm) {
      this.msgId = msgId;
      this.creDtTm = creDtTm;
    }

    public String getMsgId() {
      return msgId;
    }

    public String getCreDtTm() {
      return creDtTm;
    }
  }

  public static class OriginalGroupInfoDto {

    private final String orgnlMsgId;
    private final String orgnlMsgNmId;
    private final String grpSts;

    public OriginalGroupInfoDto(String orgnlMsgId, String orgnlMsgNmId, String grpSts) {
      this.orgnlMsgId = orgnlMsgId;
      this.orgnlMsgNmId = orgnlMsgNmId;
      this.grpSts = grpSts;
    }

    public String getOrgnlMsgId() {
      return orgnlMsgId;
    }

    public String getOrgnlMsgNmId() {
      return orgnlMsgNmId;
    }

    public String getGrpSts() {
      return grpSts;
    }
  }

  public static class OriginalPaymentInfoDto {

    private final String orgnlPmtInfId;
    private final List<TransactionStatusInfoDto> txInfAndSts;

    public OriginalPaymentInfoDto(
        String orgnlPmtInfId, List<TransactionStatusInfoDto> txInfAndSts) {
      this.orgnlPmtInfId = orgnlPmtInfId;
      this.txInfAndSts = txInfAndSts;
    }

    public String getOrgnlPmtInfId() {
      return orgnlPmtInfId;
    }

    public List<TransactionStatusInfoDto> getTxInfAndSts() {
      return txInfAndSts;
    }
  }

  public static class TransactionStatusInfoDto {

    private final String orgnlEndToEndId;
    private final TransactionStatus txSts;
    private final List<StatusReasonInformationDto> stsRsnInf;

    public TransactionStatusInfoDto(
        String orgnlEndToEndId,
        TransactionStatus txSts,
        List<StatusReasonInformationDto> stsRsnInf) {
      this.orgnlEndToEndId = orgnlEndToEndId;
      this.txSts = txSts;
      this.stsRsnInf = stsRsnInf;
    }

    public String getOrgnlEndToEndId() {
      return orgnlEndToEndId;
    }

    public TransactionStatus getTxSts() {
      return txSts;
    }

    public List<StatusReasonInformationDto> getStsRsnInf() {
      return stsRsnInf;
    }
  }

  public static class StatusReasonInformationDto {

    private final ReasonCodeDto rsn;
    private final List<String> addtlInf;

    public StatusReasonInformationDto(ReasonCodeDto rsn, List<String> addtlInf) {
      this.rsn = rsn;
      this.addtlInf = addtlInf;
    }

    public ReasonCodeDto getRsn() {
      return rsn;
    }

    public List<String> getAddtlInf() {
      return addtlInf;
    }
  }

  public static class ReasonCodeDto {

    private final String cd;

    public ReasonCodeDto(String cd) {
      this.cd = cd;
    }

    public String getCd() {
      return cd;
    }
  }
}
