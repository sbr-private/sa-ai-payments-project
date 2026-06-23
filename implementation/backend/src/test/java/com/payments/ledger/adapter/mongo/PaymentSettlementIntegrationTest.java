package com.payments.ledger.adapter.mongo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.payments.ledger.api.auth.DemoAuthInterceptor;
import com.payments.ledger.domain.model.Account;
import com.payments.ledger.domain.model.AccountStatus;
import com.payments.ledger.domain.model.Money;
import com.payments.ledger.domain.model.Party;
import com.payments.ledger.domain.money.MoneyConverter;
import com.payments.ledger.repository.LedgerRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentSettlementIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private LedgerRepository ledgerRepository;

  private UUID debtorId;
  private UUID creditorId;
  private UUID thirdAccountId;

  @BeforeEach
  void setUpAccounts() {
    debtorId = UUID.randomUUID();
    creditorId = UUID.randomUUID();
    thirdAccountId = UUID.randomUUID();

    insertAccount(debtorId, "Debtor Co");
    insertAccount(creditorId, "Creditor Co");
    insertAccount(thirdAccountId, "Third Co");
  }

  @Test
  void sc003InsufficientFundsLeavesBalancesUnchanged() throws Exception {
    fund(debtorId, "10.00");
    String endToEndId = "E2E-INT-SC003-" + UUID.randomUUID().toString().substring(0, 8);

    mockMvc
        .perform(
            post("/payment-initiations")
                .header(DemoAuthInterceptor.DEMO_USER_HEADER, "benchmark@demo")
                .contentType(APPLICATION_JSON)
                .content(pain001(endToEndId, debtorId, creditorId, "50.00")))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.orgnlPmtInfAndSts[0].txInfAndSts[0].txSts").value("RJCT"))
        .andExpect(jsonPath("$.orgnlPmtInfAndSts[0].txInfAndSts[0].stsRsnInf[0].rsn.cd")
            .value("AM04"));

    assertThat(accountBalance(debtorId)).isEqualTo(1000L);
    assertThat(accountBalance(creditorId)).isEqualTo(0L);
    assertThat(
            ledgerRepository.findStatementEntries(debtorId, 10, Optional.empty()).getEntries())
        .noneMatch(entry -> entry.getEndToEndId().equals(endToEndId));
    assertThat(
            ledgerRepository.findStatementEntries(creditorId, 10, Optional.empty()).getEntries())
        .noneMatch(entry -> entry.getEndToEndId().equals(endToEndId));
  }

  @Test
  void sc004IdempotentReplayDoesNotDoubleSettle() throws Exception {
    fund(debtorId, "1000.00");
    String endToEndId = "E2E-INT-SC004-" + UUID.randomUUID().toString().substring(0, 8);
    String body = pain001(endToEndId, debtorId, creditorId, "30.00");

    mockMvc
        .perform(
            post("/payment-initiations")
                .header(DemoAuthInterceptor.DEMO_USER_HEADER, "benchmark@demo")
                .contentType(APPLICATION_JSON)
                .content(body))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.orgnlPmtInfAndSts[0].txInfAndSts[0].txSts").value("ACSC"));

    mockMvc
        .perform(
            post("/payment-initiations")
                .header(DemoAuthInterceptor.DEMO_USER_HEADER, "benchmark@demo")
                .contentType(APPLICATION_JSON)
                .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.orgnlPmtInfAndSts[0].txInfAndSts[0].txSts").value("ACSC"));

    assertThat(accountBalance(debtorId)).isEqualTo(97000L);
    assertThat(accountBalance(creditorId)).isEqualTo(3000L);
  }

  @Test
  void sc005IdempotencyConflictReturns409() throws Exception {
    fund(debtorId, "1000.00");
    String endToEndId = "E2E-INT-SC005-" + UUID.randomUUID().toString().substring(0, 8);

    mockMvc
        .perform(
            post("/payment-initiations")
                .header(DemoAuthInterceptor.DEMO_USER_HEADER, "benchmark@demo")
                .contentType(APPLICATION_JSON)
                .content(pain001(endToEndId, debtorId, creditorId, "20.00")))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            post("/payment-initiations")
                .header(DemoAuthInterceptor.DEMO_USER_HEADER, "benchmark@demo")
                .contentType(APPLICATION_JSON)
                .content(pain001(endToEndId, debtorId, thirdAccountId, "20.00")))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.orgnlPmtInfAndSts[0].txInfAndSts[0].stsRsnInf[0].rsn.cd")
            .value("DU04"));

    assertThat(accountBalance(debtorId)).isEqualTo(98000L);
    assertThat(accountBalance(creditorId)).isEqualTo(2000L);
    assertThat(accountBalance(thirdAccountId)).isEqualTo(0L);
  }

  @Test
  void sc012TransactionLookupReturns404WhenMissing() throws Exception {
    mockMvc
        .perform(
            get("/payment-initiations/transactions/{endToEndId}", "E2E-MISSING-0001")
                .header(DemoAuthInterceptor.DEMO_USER_HEADER, "support@demo"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error.details.endToEndId").value("E2E-MISSING-0001"));
  }

  @Test
  void sc015SelfTransferRejectedWithAg01() throws Exception {
    fund(debtorId, "100.00");
    String endToEndId = "E2E-INT-SC015-" + UUID.randomUUID().toString().substring(0, 8);

    mockMvc
        .perform(
            post("/payment-initiations")
                .header(DemoAuthInterceptor.DEMO_USER_HEADER, "benchmark@demo")
                .contentType(APPLICATION_JSON)
                .content(pain001(endToEndId, debtorId, debtorId, "10.00")))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.orgnlPmtInfAndSts[0].txInfAndSts[0].stsRsnInf[0].rsn.cd")
            .value("AG01"));

    assertThat(accountBalance(debtorId)).isEqualTo(10000L);
  }

  @Test
  void rejectedTransactionLookupReturnsAm04() throws Exception {
    fund(debtorId, "10.00");
    String endToEndId = "E2E-INT-LOOKUP-" + UUID.randomUUID().toString().substring(0, 8);

    mockMvc
        .perform(
            post("/payment-initiations")
                .header(DemoAuthInterceptor.DEMO_USER_HEADER, "benchmark@demo")
                .contentType(APPLICATION_JSON)
                .content(pain001(endToEndId, debtorId, creditorId, "50.00")))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            get("/payment-initiations/transactions/{endToEndId}", endToEndId)
                .header(DemoAuthInterceptor.DEMO_USER_HEADER, "support@demo"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.txSts").value("RJCT"))
        .andExpect(jsonPath("$.stsRsnInf[0].rsn.cd").value("AM04"));
  }

  private void insertAccount(UUID id, String name) {
    ledgerRepository.insertAccount(
        new Account(
            id,
            new Party(name, Optional.empty()),
            "USD",
            MoneyConverter.zero("USD"),
            AccountStatus.active,
            Instant.parse("2026-06-23T11:00:00Z")));
  }

  private void fund(UUID accountId, String amount) {
    String endToEndId = "E2E-SETUP-" + UUID.randomUUID();
    ledgerRepository.creditAccount(
        accountId,
        new Money(MoneyConverter.toMinorUnits(amount, "USD"), "USD"),
        endToEndId);
  }

  private long accountBalance(UUID accountId) {
    return ledgerRepository.findAccountById(accountId).orElseThrow().getBalance().getValueMinor();
  }

  private static String pain001(
      String endToEndId, UUID debtor, UUID creditor, String amount) {
    return """
        {
          "grpHdr": {
            "msgId": "MSG-INT-0001",
            "creDtTm": "2026-06-23T12:00:00Z",
            "nbOfTxs": "1",
            "ctrlSum": "%s",
            "initgPty": { "nm": "Test Co" }
          },
          "pmtInf": [{
            "pmtInfId": "PMT-INT-0001",
            "pmtMtd": "TRF",
            "dbtr": { "nm": "Debtor" },
            "dbtrAcct": {
              "id": { "othr": { "id": "%s" } },
              "ccy": "USD"
            },
            "cdtTrfTxInf": [{
              "pmtId": { "endToEndId": "%s" },
              "amt": { "instdAmt": { "value": "%s", "ccy": "USD" } },
              "cdtr": { "nm": "Creditor" },
              "cdtrAcct": {
                "id": { "othr": { "id": "%s" } },
                "ccy": "USD"
              }
            }]
          }]
        }
        """
        .formatted(amount, debtor, endToEndId, amount, creditor);
  }
}
