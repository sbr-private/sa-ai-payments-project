package com.payments.ledger.domain.service;

import com.payments.ledger.api.dto.TestCreditRequest;
import com.payments.ledger.domain.exception.AccountNotFoundException;
import com.payments.ledger.domain.model.Money;
import com.payments.ledger.domain.money.MoneyConverter;
import com.payments.ledger.repository.LedgerRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class TestAccountService {

  private final LedgerRepository ledgerRepository;

  public TestAccountService(LedgerRepository ledgerRepository) {
    this.ledgerRepository = ledgerRepository;
  }

  public void credit(UUID accountId, TestCreditRequest request) {
    ensureAccountExists(accountId);
    Money amount =
        new Money(
            MoneyConverter.toMinorUnits(
                request.getAmount().getValue(), request.getAmount().getCcy()),
            request.getAmount().getCcy());
    ledgerRepository.creditAccount(accountId, amount, request.getEndToEndId());
  }

  public void close(UUID accountId) {
    ensureAccountExists(accountId);
    ledgerRepository.closeAccount(accountId);
  }

  private void ensureAccountExists(UUID accountId) {
    ledgerRepository
        .findAccountById(accountId)
        .orElseThrow(() -> new AccountNotFoundException(accountId));
  }
}
