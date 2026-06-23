package com.payments.ledger.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.payments.ledger.domain.DemoSeedData;
import com.payments.ledger.domain.model.Account;
import com.payments.ledger.domain.model.AccountStatus;
import com.payments.ledger.domain.model.Money;
import com.payments.ledger.domain.model.Party;
import com.payments.ledger.domain.model.PaymentTransaction;
import com.payments.ledger.domain.model.StatusReason;
import com.payments.ledger.domain.model.TransactionStatus;
import com.payments.ledger.domain.model.TransferCommand;
import com.payments.ledger.domain.model.TransferOutcome;
import com.payments.ledger.repository.LedgerRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SampleDataLoaderTest {

  @Mock private LedgerRepository ledgerRepository;

  @InjectMocks private SampleDataLoader sampleDataLoader;

  @Test
  void loadSkipsWhenAcmeAccountAlreadyExists() {
    when(ledgerRepository.findAccountById(DemoSeedData.ACME_ACCOUNT_ID))
        .thenReturn(
            Optional.of(
                new Account(
                    DemoSeedData.ACME_ACCOUNT_ID,
                    new Party("Acme Corp", Optional.empty()),
                    "USD",
                    new Money(95000, "USD"),
                    AccountStatus.active,
                    Instant.parse("2026-06-23T11:00:00Z"))));

    sampleDataLoader.load();

    verify(ledgerRepository, never()).insertAccount(any());
    verify(ledgerRepository, never()).creditAccount(any(), any(), any());
    verify(ledgerRepository, never()).settleTransfer(any());
  }

  @Test
  void loadInsertsAccountsAndRunsSettlementSequence() {
    when(ledgerRepository.findAccountById(DemoSeedData.ACME_ACCOUNT_ID)).thenReturn(Optional.empty());
    when(ledgerRepository.settleTransfer(any(TransferCommand.class)))
        .thenAnswer(
            invocation -> {
              TransferCommand command = invocation.getArgument(0);
              TransactionStatus status =
                  DemoSeedData.E2E_FAILED.equals(command.getEndToEndId())
                      ? TransactionStatus.RJCT
                      : TransactionStatus.ACSC;
              List<StatusReason> reasons =
                  status == TransactionStatus.RJCT
                      ? List.of(new StatusReason("AM04", List.of()))
                      : List.of();
              return TransferOutcome.created(
                  new PaymentTransaction(
                      UUID.randomUUID(),
                      command.getEndToEndId(),
                      command.getInstrId(),
                      command.getDebtorAccountId(),
                      command.getCreditorAccountId(),
                      command.getInstructedAmount(),
                      status,
                      reasons,
                      Instant.parse("2026-06-23T12:00:00Z")));
            });

    sampleDataLoader.load();

    verify(ledgerRepository, times(2)).insertAccount(any(Account.class));
    verify(ledgerRepository)
        .creditAccount(
            eq(DemoSeedData.ACME_ACCOUNT_ID), any(Money.class), eq(DemoSeedData.E2E_OPENING));
    verify(ledgerRepository, times(2)).settleTransfer(any(TransferCommand.class));
  }
}
