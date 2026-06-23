package com.payments.ledger.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.payments.ledger.api.auth.DemoAuthInterceptor;
import com.payments.ledger.domain.model.Account;
import com.payments.ledger.domain.model.AccountStatus;
import com.payments.ledger.domain.model.CreditDebitIndicator;
import com.payments.ledger.domain.model.Money;
import com.payments.ledger.domain.model.Party;
import com.payments.ledger.domain.model.StatementEntry;
import com.payments.ledger.domain.model.StatementPage;
import com.payments.ledger.repository.LedgerRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class StatementControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private LedgerRepository ledgerRepository;

  @Test
  void getStatementReturnsEntries() throws Exception {
    UUID accountId = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");

    when(ledgerRepository.findAccountById(accountId))
        .thenReturn(
            Optional.of(
                new Account(
                    accountId,
                    new Party("Acme Corp", Optional.empty()),
                    "USD",
                    new Money(95000, "USD"),
                    AccountStatus.active,
                    Instant.parse("2026-06-23T11:00:00Z"))));

    StatementEntry entry =
        new StatementEntry(
            UUID.fromString("e001a2b3-c4d5-6789-abcd-ef0123456789"),
            accountId,
            UUID.randomUUID(),
            "E2E-INV-2024-0558",
            new Money(5000, "USD"),
            CreditDebitIndicator.DBIT,
            new Money(95000, "USD"),
            LocalDate.parse("2026-06-23"),
            Instant.parse("2026-06-23T12:00:01.100Z"));

    when(ledgerRepository.findStatementEntries(eq(accountId), eq(20), any()))
        .thenReturn(new StatementPage(List.of(entry), Optional.empty(), false));

    mockMvc
        .perform(
            get("/accounts/{id}/statements", accountId)
                .header(DemoAuthInterceptor.DEMO_USER_HEADER, "benchmark@demo"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.stmt.bal[0].amt.value").value("950.00"))
        .andExpect(jsonPath("$.stmt.ntry[0].cdtDbtInd").value("DBIT"))
        .andExpect(jsonPath("$.stmt.ntry[0].amt.value").value("50.00"))
        .andExpect(jsonPath("$.hasMore").value(false));
  }
}
