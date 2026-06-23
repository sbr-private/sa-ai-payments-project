package com.payments.ledger.api.dto.statement;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.payments.ledger.api.dto.CurrencyAndAmountDto;
import com.payments.ledger.api.dto.payment.CustomerCreditTransferInitiationRequest;
import com.payments.ledger.domain.model.CreditDebitIndicator;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccountStatementResponse {

  private final StatementDto stmt;
  private final String nextCursor;
  private final boolean hasMore;

  public AccountStatementResponse(StatementDto stmt, String nextCursor, boolean hasMore) {
    this.stmt = stmt;
    this.nextCursor = nextCursor;
    this.hasMore = hasMore;
  }

  public StatementDto getStmt() {
    return stmt;
  }

  public String getNextCursor() {
    return nextCursor;
  }

  public boolean isHasMore() {
    return hasMore;
  }

  public static class StatementDto {

    private final String id;
    private final String creDtTm;
    private final AccountRefDto acct;
    private final List<BalanceDto> bal;
    private final List<EntryDto> ntry;

    public StatementDto(
        String id,
        String creDtTm,
        AccountRefDto acct,
        List<BalanceDto> bal,
        List<EntryDto> ntry) {
      this.id = id;
      this.creDtTm = creDtTm;
      this.acct = acct;
      this.bal = bal;
      this.ntry = ntry;
    }

    public String getId() {
      return id;
    }

    public String getCreDtTm() {
      return creDtTm;
    }

    public AccountRefDto getAcct() {
      return acct;
    }

    public List<BalanceDto> getBal() {
      return bal;
    }

    public List<EntryDto> getNtry() {
      return ntry;
    }
  }

  public static class AccountRefDto {

    private final CustomerCreditTransferInitiationRequest.AccountIdDto id;
    private final String ccy;

    public AccountRefDto(
        CustomerCreditTransferInitiationRequest.AccountIdDto id, String ccy) {
      this.id = id;
      this.ccy = ccy;
    }

    public CustomerCreditTransferInitiationRequest.AccountIdDto getId() {
      return id;
    }

    public String getCcy() {
      return ccy;
    }
  }

  public static class BalanceDto {

    private final TypeDto tp;
    private final CurrencyAndAmountDto amt;
    private final CreditDebitIndicator cdtDbtInd;
    private final DateDto dt;

    public BalanceDto(
        TypeDto tp,
        CurrencyAndAmountDto amt,
        CreditDebitIndicator cdtDbtInd,
        DateDto dt) {
      this.tp = tp;
      this.amt = amt;
      this.cdtDbtInd = cdtDbtInd;
      this.dt = dt;
    }

    public TypeDto getTp() {
      return tp;
    }

    public CurrencyAndAmountDto getAmt() {
      return amt;
    }

    public CreditDebitIndicator getCdtDbtInd() {
      return cdtDbtInd;
    }

    public DateDto getDt() {
      return dt;
    }
  }

  public static class TypeDto {

    private final CodeOrProprietaryDto cdOrPrtry;

    public TypeDto(CodeOrProprietaryDto cdOrPrtry) {
      this.cdOrPrtry = cdOrPrtry;
    }

    public CodeOrProprietaryDto getCdOrPrtry() {
      return cdOrPrtry;
    }
  }

  public static class CodeOrProprietaryDto {

    private final String cd;

    public CodeOrProprietaryDto(String cd) {
      this.cd = cd;
    }

    public String getCd() {
      return cd;
    }
  }

  public static class DateDto {

    private final String dt;

    public DateDto(String dt) {
      this.dt = dt;
    }

    public String getDt() {
      return dt;
    }
  }

  public static class EntryDto {

    private final String ntryRef;
    private final CurrencyAndAmountDto amt;
    private final CreditDebitIndicator cdtDbtInd;
    private final String sts;
    private final DateDto bookgDt;
    private final List<EntryDetailsDto> ntryDtls;

    public EntryDto(
        String ntryRef,
        CurrencyAndAmountDto amt,
        CreditDebitIndicator cdtDbtInd,
        String sts,
        DateDto bookgDt,
        List<EntryDetailsDto> ntryDtls) {
      this.ntryRef = ntryRef;
      this.amt = amt;
      this.cdtDbtInd = cdtDbtInd;
      this.sts = sts;
      this.bookgDt = bookgDt;
      this.ntryDtls = ntryDtls;
    }

    public String getNtryRef() {
      return ntryRef;
    }

    public CurrencyAndAmountDto getAmt() {
      return amt;
    }

    public CreditDebitIndicator getCdtDbtInd() {
      return cdtDbtInd;
    }

    public String getSts() {
      return sts;
    }

    public DateDto getBookgDt() {
      return bookgDt;
    }

    public List<EntryDetailsDto> getNtryDtls() {
      return ntryDtls;
    }
  }

  public static class EntryDetailsDto {

    private final List<TransactionDetailsDto> txDtls;

    public EntryDetailsDto(List<TransactionDetailsDto> txDtls) {
      this.txDtls = txDtls;
    }

    public List<TransactionDetailsDto> getTxDtls() {
      return txDtls;
    }
  }

  public static class TransactionDetailsDto {

    private final RefsDto refs;

    public TransactionDetailsDto(RefsDto refs) {
      this.refs = refs;
    }

    public RefsDto getRefs() {
      return refs;
    }
  }

  public static class RefsDto {

    private final String endToEndId;

    public RefsDto(String endToEndId) {
      this.endToEndId = endToEndId;
    }

    public String getEndToEndId() {
      return endToEndId;
    }
  }
}
