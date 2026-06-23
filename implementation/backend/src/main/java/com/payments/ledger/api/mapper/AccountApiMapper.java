package com.payments.ledger.api.mapper;

import com.payments.ledger.api.dto.AccountResponse;
import com.payments.ledger.api.dto.CurrencyAndAmountDto;
import com.payments.ledger.api.dto.RegisterAccountRequest;
import com.payments.ledger.domain.model.Account;
import com.payments.ledger.domain.money.MoneyConverter;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

@Component
public class AccountApiMapper {

  private static final DateTimeFormatter CRE_DT_TM_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(java.time.ZoneOffset.UTC);

  public AccountResponse toResponse(Account account) {
    RegisterAccountRequest.PartyIdentificationDto owner = new RegisterAccountRequest.PartyIdentificationDto();
    owner.setNm(account.getOwner().getName());
    account
        .getOwner()
        .getExternalId()
        .ifPresent(
            externalId -> {
              RegisterAccountRequest.OtherIdDto othr = new RegisterAccountRequest.OtherIdDto();
              othr.setId(externalId);
              RegisterAccountRequest.PartyIdDto id = new RegisterAccountRequest.PartyIdDto();
              id.setOthr(othr);
              owner.setId(id);
            });

    CurrencyAndAmountDto balance =
        new CurrencyAndAmountDto(
            MoneyConverter.toDecimalString(account.getBalance()), account.getBalance().getCcy());

    return new AccountResponse(
        account.getId().toString(),
        owner,
        account.getCcy(),
        balance,
        account.getStatus(),
        CRE_DT_TM_FORMATTER.format(account.getCreatedAt()));
  }
}
