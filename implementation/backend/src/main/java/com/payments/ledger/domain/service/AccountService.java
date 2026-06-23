package com.payments.ledger.domain.service;

import com.payments.ledger.domain.model.Account;
import com.payments.ledger.domain.model.AccountStatus;
import com.payments.ledger.domain.model.Party;
import com.payments.ledger.domain.money.MoneyConverter;
import com.payments.ledger.repository.LedgerRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AccountService {

  private final LedgerRepository ledgerRepository;

  public AccountService(LedgerRepository ledgerRepository) {
    this.ledgerRepository = ledgerRepository;
  }

  public Account registerAccount(Party owner, String ccy) {
    Account account =
        new Account(
            UUID.randomUUID(),
            owner,
            ccy,
            MoneyConverter.zero(ccy),
            AccountStatus.active,
            Instant.now());

    return ledgerRepository.insertAccount(account);
  }
}
