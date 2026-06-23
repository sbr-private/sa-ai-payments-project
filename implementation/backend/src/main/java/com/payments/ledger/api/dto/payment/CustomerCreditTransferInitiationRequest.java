package com.payments.ledger.api.dto.payment;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.payments.ledger.api.dto.CurrencyAndAmountDto;
import com.payments.ledger.api.dto.RegisterAccountRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class CustomerCreditTransferInitiationRequest {

  @NotNull @Valid private GroupHeaderDto grpHdr;

  @NotEmpty @Valid private List<PaymentInstructionDto> pmtInf;

  public GroupHeaderDto getGrpHdr() {
    return grpHdr;
  }

  public void setGrpHdr(GroupHeaderDto grpHdr) {
    this.grpHdr = grpHdr;
  }

  public List<PaymentInstructionDto> getPmtInf() {
    return pmtInf;
  }

  public void setPmtInf(List<PaymentInstructionDto> pmtInf) {
    this.pmtInf = pmtInf;
  }

  public static class GroupHeaderDto {

    @NotBlank private String msgId;
    @NotBlank private String creDtTm;
    @NotBlank private String nbOfTxs;
    @NotBlank private String ctrlSum;
    @NotNull @Valid private RegisterAccountRequest.PartyIdentificationDto initgPty;

    public String getMsgId() {
      return msgId;
    }

    public void setMsgId(String msgId) {
      this.msgId = msgId;
    }

    public String getCreDtTm() {
      return creDtTm;
    }

    public void setCreDtTm(String creDtTm) {
      this.creDtTm = creDtTm;
    }

    public String getNbOfTxs() {
      return nbOfTxs;
    }

    public void setNbOfTxs(String nbOfTxs) {
      this.nbOfTxs = nbOfTxs;
    }

    public String getCtrlSum() {
      return ctrlSum;
    }

    public void setCtrlSum(String ctrlSum) {
      this.ctrlSum = ctrlSum;
    }

    public RegisterAccountRequest.PartyIdentificationDto getInitgPty() {
      return initgPty;
    }

    public void setInitgPty(RegisterAccountRequest.PartyIdentificationDto initgPty) {
      this.initgPty = initgPty;
    }
  }

  public static class PaymentInstructionDto {

    @NotBlank private String pmtInfId;
    @NotBlank private String pmtMtd;
    @NotNull @Valid private RegisterAccountRequest.PartyIdentificationDto dbtr;
    @NotNull @Valid private AccountIdentificationDto dbtrAcct;
    @NotEmpty @Valid private List<CreditTransferTransactionDto> cdtTrfTxInf;

    public String getPmtInfId() {
      return pmtInfId;
    }

    public void setPmtInfId(String pmtInfId) {
      this.pmtInfId = pmtInfId;
    }

    public String getPmtMtd() {
      return pmtMtd;
    }

    public void setPmtMtd(String pmtMtd) {
      this.pmtMtd = pmtMtd;
    }

    public RegisterAccountRequest.PartyIdentificationDto getDbtr() {
      return dbtr;
    }

    public void setDbtr(RegisterAccountRequest.PartyIdentificationDto dbtr) {
      this.dbtr = dbtr;
    }

    public AccountIdentificationDto getDbtrAcct() {
      return dbtrAcct;
    }

    public void setDbtrAcct(AccountIdentificationDto dbtrAcct) {
      this.dbtrAcct = dbtrAcct;
    }

    public List<CreditTransferTransactionDto> getCdtTrfTxInf() {
      return cdtTrfTxInf;
    }

    public void setCdtTrfTxInf(List<CreditTransferTransactionDto> cdtTrfTxInf) {
      this.cdtTrfTxInf = cdtTrfTxInf;
    }
  }

  public static class AccountIdentificationDto {

    @NotNull @Valid private AccountIdDto id;
    private String ccy;

    public AccountIdDto getId() {
      return id;
    }

    public void setId(AccountIdDto id) {
      this.id = id;
    }

    public String getCcy() {
      return ccy;
    }

    public void setCcy(String ccy) {
      this.ccy = ccy;
    }
  }

  public static class AccountIdDto {

    @NotNull @Valid private OtherIdDto othr;

    public OtherIdDto getOthr() {
      return othr;
    }

    public void setOthr(OtherIdDto othr) {
      this.othr = othr;
    }
  }

  public static class OtherIdDto {

    @NotBlank private String id;

    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }
  }

  public static class CreditTransferTransactionDto {

    @NotNull @Valid private PaymentIdentificationDto pmtId;
    @NotNull @Valid private AmountDto amt;
    @NotNull @Valid private RegisterAccountRequest.PartyIdentificationDto cdtr;
    @NotNull @Valid private AccountIdentificationDto cdtrAcct;
    @Valid private RemittanceInformationDto rmtInf;

    public PaymentIdentificationDto getPmtId() {
      return pmtId;
    }

    public void setPmtId(PaymentIdentificationDto pmtId) {
      this.pmtId = pmtId;
    }

    public AmountDto getAmt() {
      return amt;
    }

    public void setAmt(AmountDto amt) {
      this.amt = amt;
    }

    public RegisterAccountRequest.PartyIdentificationDto getCdtr() {
      return cdtr;
    }

    public void setCdtr(RegisterAccountRequest.PartyIdentificationDto cdtr) {
      this.cdtr = cdtr;
    }

    public AccountIdentificationDto getCdtrAcct() {
      return cdtrAcct;
    }

    public void setCdtrAcct(AccountIdentificationDto cdtrAcct) {
      this.cdtrAcct = cdtrAcct;
    }

    public RemittanceInformationDto getRmtInf() {
      return rmtInf;
    }

    public void setRmtInf(RemittanceInformationDto rmtInf) {
      this.rmtInf = rmtInf;
    }
  }

  public static class PaymentIdentificationDto {

    private String instrId;

    @NotBlank private String endToEndId;

    public String getInstrId() {
      return instrId;
    }

    public void setInstrId(String instrId) {
      this.instrId = instrId;
    }

    public String getEndToEndId() {
      return endToEndId;
    }

    public void setEndToEndId(String endToEndId) {
      this.endToEndId = endToEndId;
    }
  }

  public static class AmountDto {

    @NotNull @Valid private CurrencyAndAmountDto instdAmt;

    public CurrencyAndAmountDto getInstdAmt() {
      return instdAmt;
    }

    public void setInstdAmt(CurrencyAndAmountDto instdAmt) {
      this.instdAmt = instdAmt;
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class RemittanceInformationDto {

    private List<String> ustrd;

    public List<String> getUstrd() {
      return ustrd;
    }

    public void setUstrd(List<String> ustrd) {
      this.ustrd = ustrd;
    }
  }
}
