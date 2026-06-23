package com.payments.ledger.config;

import com.payments.ledger.domain.DemoSeedData;
import com.payments.ledger.domain.model.Account;
import com.payments.ledger.domain.model.AccountStatus;
import com.payments.ledger.domain.model.Money;
import com.payments.ledger.domain.model.Party;
import com.payments.ledger.domain.model.TransactionStatus;
import com.payments.ledger.domain.model.TransferCommand;
import com.payments.ledger.domain.model.TransferOutcome;
import com.payments.ledger.domain.money.MoneyConverter;
import com.payments.ledger.repository.LedgerRepository;
import java.time.Instant;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Loads the minimal demo ledger from docs/SEED.md: Acme ($950), Supplier ($50), and a
 * rejected payment (E2E-INV-2024-0999 / AM04) for support investigations.
 */
@Component
public class SampleDataLoader {

  private static final Logger log = LoggerFactory.getLogger(SampleDataLoader.class);

  private static final Instant SEED_CREATED_AT = Instant.parse("2026-06-23T11:00:00Z");

  private final LedgerRepository ledgerRepository;

  public SampleDataLoader(LedgerRepository ledgerRepository) {
    this.ledgerRepository = ledgerRepository;
  }

  public void load() {
    if (ledgerRepository.findAccountById(DemoSeedData.ACME_ACCOUNT_ID).isPresent()) {
      log.info("Sample data already loaded (Acme account present) — skipping");
      return;
    }

    log.info("Loading minimal demo sample data");

    ledgerRepository.insertAccount(acmeAccount());
    ledgerRepository.insertAccount(supplierAccount());

    ledgerRepository.creditAccount(
        DemoSeedData.ACME_ACCOUNT_ID,
        money("1000.00", "USD"),
        DemoSeedData.E2E_OPENING);

    TransferOutcome success =
        ledgerRepository.settleTransfer(
            new TransferCommand(
                DemoSeedData.E2E_SUCCESS,
                Optional.of(DemoSeedData.INSTR_SUCCESS),
                DemoSeedData.ACME_ACCOUNT_ID,
                DemoSeedData.SUPPLIER_ACCOUNT_ID,
                money("50.00", "USD")));

    TransferOutcome failed =
        ledgerRepository.settleTransfer(
            new TransferCommand(
                DemoSeedData.E2E_FAILED,
                Optional.of(DemoSeedData.INSTR_FAILED),
                DemoSeedData.ACME_ACCOUNT_ID,
                DemoSeedData.SUPPLIER_ACCOUNT_ID,
                money("2000.00", "USD")));

    if (success.getTransaction().getStatus() != TransactionStatus.ACSC) {
      throw new IllegalStateException(
          "Expected ACSC for " + DemoSeedData.E2E_SUCCESS + " during sample data load");
    }
    if (failed.getTransaction().getStatus() != TransactionStatus.RJCT) {
      throw new IllegalStateException(
          "Expected RJCT for " + DemoSeedData.E2E_FAILED + " during sample data load");
    }

    log.info(
        "Sample data loaded — payer@demo Acme balance $950.00, Supplier $50.00, failed payment {}",
        DemoSeedData.E2E_FAILED);
  }

  private static Account acmeAccount() {
    return new Account(
        DemoSeedData.ACME_ACCOUNT_ID,
        new Party("Acme Corp", Optional.of("user_123")),
        "USD",
        MoneyConverter.zero("USD"),
        AccountStatus.active,
        SEED_CREATED_AT);
  }

  private static Account supplierAccount() {
    return new Account(
        DemoSeedData.SUPPLIER_ACCOUNT_ID,
        new Party("Supplier Ltd", Optional.empty()),
        "USD",
        MoneyConverter.zero("USD"),
        AccountStatus.active,
        SEED_CREATED_AT);
  }

  private static Money money(String value, String ccy) {
    return new Money(MoneyConverter.toMinorUnits(value, ccy), ccy);
  }
}
