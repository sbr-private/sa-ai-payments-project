package com.payments.ledger.adapter.mongo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payments.ledger.api.auth.DemoAuthInterceptor;
import com.payments.ledger.domain.model.Account;
import com.payments.ledger.domain.model.AccountStatus;
import com.payments.ledger.domain.model.Money;
import com.payments.ledger.domain.model.Party;
import com.payments.ledger.domain.money.MoneyConverter;
import com.payments.ledger.repository.LedgerRepository;
import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class StatementPaginationIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private LedgerRepository ledgerRepository;

  @Autowired private ObjectMapper objectMapper;

  private UUID debtorId;
  private UUID creditorId;

  @BeforeEach
  void setUp() throws Exception {
    debtorId = UUID.randomUUID();
    creditorId = UUID.randomUUID();
    insertAccount(debtorId, "Debtor Co");
    insertAccount(creditorId, "Creditor Co");
    fund(debtorId, "100.00");

    for (int i = 0; i < 5; i++) {
      String endToEndId = "E2E-PAGE-" + UUID.randomUUID().toString().substring(0, 8);
      postPayment(endToEndId, "10.00");
    }
  }

  @Test
  void sc011PaginatesNewestFirstWithoutOverlap() throws Exception {
    MvcResult page1 =
        mockMvc
            .perform(
                get("/accounts/{id}/statements", debtorId)
                    .param("limit", "2")
                    .header(DemoAuthInterceptor.DEMO_USER_HEADER, "benchmark@demo"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.stmt.ntry.length()").value(2))
            .andExpect(jsonPath("$.hasMore").value(true))
            .andExpect(jsonPath("$.nextCursor").isNotEmpty())
            .andReturn();

    String cursor =
        objectMapper.readTree(page1.getResponse().getContentAsString()).get("nextCursor").asText();
    Set<String> seen = entryRefs(page1);

    MvcResult page2 =
        mockMvc
            .perform(
                get("/accounts/{id}/statements", debtorId)
                    .param("limit", "2")
                    .param("cursor", cursor)
                    .header(DemoAuthInterceptor.DEMO_USER_HEADER, "benchmark@demo"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.stmt.ntry.length()").value(2))
            .andExpect(jsonPath("$.hasMore").value(true))
            .andReturn();

    Set<String> seenPage2 = entryRefs(page2);
    for (String ref : seen) {
      assertThat(seenPage2).doesNotContain(ref);
    }
    seen.addAll(seenPage2);

    cursor =
        objectMapper.readTree(page2.getResponse().getContentAsString()).get("nextCursor").asText();

    MvcResult page3 =
        mockMvc
            .perform(
                get("/accounts/{id}/statements", debtorId)
                    .param("limit", "2")
                    .param("cursor", cursor)
                    .header(DemoAuthInterceptor.DEMO_USER_HEADER, "benchmark@demo"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.stmt.ntry.length()").value(2))
            .andExpect(jsonPath("$.hasMore").value(false))
            .andExpect(jsonPath("$.nextCursor").doesNotExist())
            .andReturn();

    Set<String> seenPage3 = entryRefs(page3);
    for (String ref : seen) {
      assertThat(seenPage3).doesNotContain(ref);
    }
    assertThat(seen.size() + seenPage3.size()).isEqualTo(6);
    assertThat(countPaymentEntries()).isEqualTo(5);
  }

  private long countPaymentEntries() {
    return ledgerRepository.findStatementEntries(debtorId, 100, Optional.empty()).getEntries().stream()
        .filter(entry -> entry.getEndToEndId().startsWith("E2E-PAGE-"))
        .count();
  }

  @Test
  void invalidLimitReturnsValidationError() throws Exception {
    mockMvc
        .perform(
            get("/accounts/{id}/statements", debtorId)
                .param("limit", "0")
                .header(DemoAuthInterceptor.DEMO_USER_HEADER, "benchmark@demo"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));

    mockMvc
        .perform(
            get("/accounts/{id}/statements", debtorId)
                .param("limit", "101")
                .header(DemoAuthInterceptor.DEMO_USER_HEADER, "benchmark@demo"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
  }

  private Set<String> entryRefs(MvcResult result) throws Exception {
    JsonNode entries = objectMapper.readTree(result.getResponse().getContentAsString())
        .path("stmt")
        .path("ntry");
    Set<String> refs = new HashSet<>();
    entries.forEach(node -> refs.add(node.get("ntryRef").asText()));
    return refs;
  }

  private void postPayment(String endToEndId, String amount) throws Exception {
    mockMvc
        .perform(
            post("/payment-initiations")
                .header(DemoAuthInterceptor.DEMO_USER_HEADER, "benchmark@demo")
                .contentType(APPLICATION_JSON)
                .content(pain001(endToEndId, debtorId, creditorId, amount)))
        .andExpect(status().isCreated());
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
    ledgerRepository.creditAccount(
        accountId,
        new Money(MoneyConverter.toMinorUnits(amount, "USD"), "USD"),
        "E2E-SETUP-" + UUID.randomUUID());
  }

  private static String pain001(
      String endToEndId, UUID debtor, UUID creditor, String amount) {
    return """
        {
          "grpHdr": {
            "msgId": "MSG-PAGE-0001",
            "creDtTm": "2026-06-23T12:00:00Z",
            "nbOfTxs": "1",
            "ctrlSum": "%s",
            "initgPty": { "nm": "Test Co" }
          },
          "pmtInf": [{
            "pmtInfId": "PMT-PAGE-0001",
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
