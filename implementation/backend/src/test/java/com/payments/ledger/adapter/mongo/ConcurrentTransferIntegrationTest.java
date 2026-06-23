package com.payments.ledger.adapter.mongo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payments.ledger.api.auth.DemoAuthInterceptor;
import com.payments.ledger.domain.model.Account;
import com.payments.ledger.domain.model.AccountStatus;
import com.payments.ledger.domain.model.Money;
import com.payments.ledger.domain.model.Party;
import com.payments.ledger.domain.model.TransactionStatus;
import com.payments.ledger.domain.money.MoneyConverter;
import com.payments.ledger.repository.LedgerRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class ConcurrentTransferIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private LedgerRepository ledgerRepository;

  @Autowired private ObjectMapper objectMapper;

  private UUID debtorId;
  private UUID creditorId;

  @BeforeEach
  void setUp() {
    debtorId = UUID.randomUUID();
    creditorId = UUID.randomUUID();
    insertAccount(debtorId, "Debtor Co");
    insertAccount(creditorId, "Creditor Co");
    fund(debtorId, "100.00");
  }

  @Test
  void sc010ConcurrentDebitsSettleOnceAndRejectOnce() throws Exception {
    ExecutorService executor = Executors.newFixedThreadPool(2);
    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);
    List<Future<MvcResult>> futures = new ArrayList<>();

    futures.add(
        executor.submit(
            () -> runPayment(ready, start, "E2E-SC10-A-" + UUID.randomUUID().toString().substring(0, 8))));
    futures.add(
        executor.submit(
            () -> runPayment(ready, start, "E2E-SC10-B-" + UUID.randomUUID().toString().substring(0, 8))));

    ready.await();
    start.countDown();
    executor.shutdown();

    List<JsonNode> statuses = new ArrayList<>();
    for (Future<MvcResult> future : futures) {
      JsonNode txStatus =
          objectMapper
              .readTree(future.get().getResponse().getContentAsString())
              .path("orgnlPmtInfAndSts")
              .get(0)
              .path("txInfAndSts")
              .get(0);
      statuses.add(txStatus);
    }

    long acscCount =
        statuses.stream().filter(node -> "ACSC".equals(node.get("txSts").asText())).count();
    long rjctCount =
        statuses.stream().filter(node -> "RJCT".equals(node.get("txSts").asText())).count();

    assertThat(acscCount).isEqualTo(1);
    assertThat(rjctCount).isEqualTo(1);
    assertThat(
            statuses.stream()
                .filter(node -> "RJCT".equals(node.get("txSts").asText()))
                .findFirst()
                .orElseThrow()
                .path("stsRsnInf")
                .get(0)
                .path("rsn")
                .path("cd")
                .asText())
        .isEqualTo("AM04");

    assertThat(accountBalance(debtorId)).isEqualTo(2000L);
    assertThat(accountBalance(creditorId)).isEqualTo(8000L);
    assertThat(countEntriesWithPrefix(debtorId, "E2E-SC10")).isEqualTo(1);
    assertThat(countEntriesWithPrefix(creditorId, "E2E-SC10")).isEqualTo(1);
  }

  @Test
  void concurrentSameEndToEndIdSettlesOnce() throws Exception {
    String endToEndId = "E2E-SC10-SAME-" + UUID.randomUUID().toString().substring(0, 8);
    ExecutorService executor = Executors.newFixedThreadPool(2);
    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);
    List<Future<MvcResult>> futures = new ArrayList<>();

    futures.add(executor.submit(() -> runPayment(ready, start, endToEndId)));
    futures.add(executor.submit(() -> runPayment(ready, start, endToEndId)));

    ready.await();
    start.countDown();
    executor.shutdown();

    List<String> endToEndIds = new ArrayList<>();
    for (Future<MvcResult> future : futures) {
      JsonNode txStatus =
          objectMapper
              .readTree(future.get().getResponse().getContentAsString())
              .path("orgnlPmtInfAndSts")
              .get(0)
              .path("txInfAndSts")
              .get(0);
      endToEndIds.add(txStatus.get("orgnlEndToEndId").asText());
      assertThat(txStatus.get("txSts").asText()).isEqualTo(TransactionStatus.ACSC.name());
    }

    assertThat(endToEndIds).containsOnly(endToEndId);
    assertThat(accountBalance(debtorId)).isEqualTo(2000L);
    assertThat(accountBalance(creditorId)).isEqualTo(8000L);
    assertThat(ledgerRepository.findByEndToEndId(endToEndId)).isPresent();
  }

  private long countEntriesWithPrefix(UUID accountId, String prefix) {
    return ledgerRepository.findStatementEntries(accountId, 100, Optional.empty()).getEntries().stream()
        .filter(entry -> entry.getEndToEndId().startsWith(prefix))
        .count();
  }

  private MvcResult runPayment(CountDownLatch ready, CountDownLatch start, String endToEndId)
      throws Exception {
    ready.countDown();
    start.await();
    return mockMvc
        .perform(
            post("/payment-initiations")
                .header(DemoAuthInterceptor.DEMO_USER_HEADER, "benchmark@demo")
                .contentType(APPLICATION_JSON)
                .content(pain001(endToEndId, debtorId, creditorId, "80.00")))
        .andReturn();
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

  private long accountBalance(UUID accountId) {
    return ledgerRepository.findAccountById(accountId).orElseThrow().getBalance().getValueMinor();
  }

  private static String pain001(
      String endToEndId, UUID debtor, UUID creditor, String amount) {
    return """
        {
          "grpHdr": {
            "msgId": "MSG-CONC-0001",
            "creDtTm": "2026-06-23T12:00:00Z",
            "nbOfTxs": "1",
            "ctrlSum": "%s",
            "initgPty": { "nm": "Test Co" }
          },
          "pmtInf": [{
            "pmtInfId": "PMT-CONC-0001",
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
