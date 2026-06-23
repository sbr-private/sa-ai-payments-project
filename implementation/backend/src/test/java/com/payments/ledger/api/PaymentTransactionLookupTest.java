package com.payments.ledger.api;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.payments.ledger.api.auth.DemoAuthInterceptor;
import com.payments.ledger.domain.model.Money;
import com.payments.ledger.domain.model.PaymentTransaction;
import com.payments.ledger.domain.model.StatusReason;
import com.payments.ledger.domain.model.TransactionStatus;
import com.payments.ledger.repository.LedgerRepository;
import java.time.Instant;
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
class PaymentTransactionLookupTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private LedgerRepository ledgerRepository;

  @Test
  void getTransactionReturnsStatusForSupport() throws Exception {
    UUID debtor = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
    UUID creditor = UUID.fromString("b2c3d4e5-f6a7-8901-bcde-f12345678901");

    when(ledgerRepository.findByEndToEndId("E2E-INV-2024-0999"))
        .thenReturn(
            Optional.of(
                new PaymentTransaction(
                    UUID.randomUUID(),
                    "E2E-INV-2024-0999",
                    Optional.of("INSTR-20260623-0002"),
                    debtor,
                    creditor,
                    new Money(200000, "USD"),
                    TransactionStatus.RJCT,
                    List.of(
                        new StatusReason(
                            "AM04", List.of("Insufficient funds on debtor account"))),
                    Instant.parse("2026-06-23T12:15:00Z"))));

    mockMvc
        .perform(
            get("/payment-initiations/transactions/{endToEndId}", "E2E-INV-2024-0999")
                .header(DemoAuthInterceptor.DEMO_USER_HEADER, "support@demo"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.orgnlEndToEndId").value("E2E-INV-2024-0999"))
        .andExpect(jsonPath("$.txSts").value("RJCT"))
        .andExpect(jsonPath("$.stsRsnInf[0].rsn.cd").value("AM04"));
  }

  @Test
  void getTransactionReturnsNotFoundWhenMissing() throws Exception {
    when(ledgerRepository.findByEndToEndId("E2E-MISSING-0001")).thenReturn(Optional.empty());

    mockMvc
        .perform(
            get("/payment-initiations/transactions/{endToEndId}", "E2E-MISSING-0001")
                .header(DemoAuthInterceptor.DEMO_USER_HEADER, "support@demo"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error.code").value("NOT_FOUND"))
        .andExpect(jsonPath("$.error.details.resource").value("transaction"))
        .andExpect(jsonPath("$.error.details.endToEndId").value("E2E-MISSING-0001"));
  }

  @Test
  void getTransactionForbiddenWhenPayerViewsUnrelatedPayment() throws Exception {
    UUID otherDebtor = UUID.fromString("00000000-0000-0000-0000-000000000099");
    UUID creditor = UUID.fromString("b2c3d4e5-f6a7-8901-bcde-f12345678901");

    when(ledgerRepository.findByEndToEndId("E2E-OTHER-0001"))
        .thenReturn(
            Optional.of(
                new PaymentTransaction(
                    UUID.randomUUID(),
                    "E2E-OTHER-0001",
                    Optional.empty(),
                    otherDebtor,
                    creditor,
                    new Money(5000, "USD"),
                    TransactionStatus.ACSC,
                    List.of(),
                    Instant.parse("2026-06-23T12:00:00Z"))));

    mockMvc
        .perform(
            get("/payment-initiations/transactions/{endToEndId}", "E2E-OTHER-0001")
                .header(DemoAuthInterceptor.DEMO_USER_HEADER, "payer@demo"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
  }
}
