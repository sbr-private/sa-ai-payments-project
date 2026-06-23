package com.payments.ledger.domain.service;

import com.payments.ledger.api.dto.statement.AccountStatementResponse;
import com.payments.ledger.domain.auth.AccountAccess;
import com.payments.ledger.domain.auth.DemoUser;
import com.payments.ledger.domain.exception.AccountNotFoundException;
import com.payments.ledger.domain.model.Account;
import com.payments.ledger.domain.model.CreditDebitIndicator;
import com.payments.ledger.domain.model.StatementEntry;
import com.payments.ledger.domain.model.StatementPage;
import com.payments.ledger.domain.money.MoneyConverter;
import com.payments.ledger.repository.LedgerRepository;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class StatementService {

  private static final DateTimeFormatter CRE_DT_TM_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC);

  private final LedgerRepository ledgerRepository;

  public StatementService(LedgerRepository ledgerRepository) {
    this.ledgerRepository = ledgerRepository;
  }

  public AccountStatementResponse getStatement(
      UUID accountId, DemoUser requester, int limit, Optional<String> cursor) {
    AccountAccess.requireCanView(requester, accountId);

    Account account =
        ledgerRepository
            .findAccountById(accountId)
            .orElseThrow(() -> new AccountNotFoundException(accountId));

    int effectiveLimit = limit <= 0 ? 20 : limit;
    StatementPage page =
        ledgerRepository.findStatementEntries(accountId, effectiveLimit, cursor);

    String stmtId =
        "STMT-"
            + DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC).format(Instant.now())
            + "-"
            + accountId.toString().substring(0, 8);

    CreditDebitIndicator closingIndicator =
        account.getBalance().getValueMinor() >= 0
            ? CreditDebitIndicator.CRDT
            : CreditDebitIndicator.DBIT;

    AccountStatementResponse.AccountRefDto acctRef = toAccountRef(account);

    AccountStatementResponse.BalanceDto closingBalance =
        new AccountStatementResponse.BalanceDto(
            new AccountStatementResponse.TypeDto(
                new AccountStatementResponse.CodeOrProprietaryDto("CLBD")),
            new com.payments.ledger.api.dto.CurrencyAndAmountDto(
                MoneyConverter.toDecimalString(account.getBalance()),
                account.getBalance().getCcy()),
            closingIndicator,
            new AccountStatementResponse.DateDto(
                DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneOffset.UTC).format(Instant.now())));

    List<AccountStatementResponse.EntryDto> entries =
        page.getEntries().stream().map(this::toEntry).toList();

    AccountStatementResponse.StatementDto stmt =
        new AccountStatementResponse.StatementDto(
            stmtId,
            CRE_DT_TM_FORMATTER.format(Instant.now()),
            acctRef,
            List.of(closingBalance),
            entries);

    return new AccountStatementResponse(
        stmt, page.getNextCursor().orElse(null), page.isHasMore());
  }

  private AccountStatementResponse.AccountRefDto toAccountRef(Account account) {
    com.payments.ledger.api.dto.payment.CustomerCreditTransferInitiationRequest.OtherIdDto othr =
        new com.payments.ledger.api.dto.payment.CustomerCreditTransferInitiationRequest.OtherIdDto();
    othr.setId(account.getId().toString());
    com.payments.ledger.api.dto.payment.CustomerCreditTransferInitiationRequest.AccountIdDto id =
        new com.payments.ledger.api.dto.payment.CustomerCreditTransferInitiationRequest.AccountIdDto();
    id.setOthr(othr);
    return new AccountStatementResponse.AccountRefDto(id, account.getCcy());
  }

  private AccountStatementResponse.EntryDto toEntry(StatementEntry entry) {
    return new AccountStatementResponse.EntryDto(
        entry.getEntryRef().toString(),
        new com.payments.ledger.api.dto.CurrencyAndAmountDto(
            MoneyConverter.toDecimalString(entry.getAmount()), entry.getAmount().getCcy()),
        entry.getCreditDebitIndicator(),
        "BOOK",
        new AccountStatementResponse.DateDto(entry.getBookingDate().toString()),
        List.of(
            new AccountStatementResponse.EntryDetailsDto(
                List.of(
                    new AccountStatementResponse.TransactionDetailsDto(
                        new AccountStatementResponse.RefsDto(entry.getEndToEndId()))))));
  }
}
