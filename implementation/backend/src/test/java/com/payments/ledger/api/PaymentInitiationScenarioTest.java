package com.payments.ledger.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.payments.ledger.api.auth.DemoAuthInterceptor;
import com.payments.ledger.domain.model.Money;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentInitiationScenarioTest {

  private static final UUID DEBTOR = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
  private static final UUID CREDITOR = UUID.fromString("b2c3d4e5-f6a7-8901-bcde-f12345678901");
  private static final UUID OTHER_CREDITOR =
      UUID.fromString("00000000-0000-0000-0000-000000000002");

  @Autowired private MockMvc mockMvc;

  @MockitoBean private LedgerRepository ledgerRepository;

  @Test
  void insufficientFundsReturns201WithAm04() throws Exception {
    when(ledgerRepository.settleTransfer(any(TransferCommand.class)))
        .thenReturn(
            TransferOutcome.created(
                rejected("E2E-SC003-0001", "AM04", "Insufficient funds on debtor account")));

    mockMvc
        .perform(
            post("/payment-initiations")
                .header(DemoAuthInterceptor.DEMO_USER_HEADER, "payer@demo")
                .contentType(APPLICATION_JSON)
                .content(pain001("E2E-SC003-0001", DEBTOR, CREDITOR, "50.00")))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.orgnlGrpInfAndSts.grpSts").value("RJCT"))
        .andExpect(jsonPath("$.orgnlPmtInfAndSts[0].txInfAndSts[0].txSts").value("RJCT"))
        .andExpect(jsonPath("$.orgnlPmtInfAndSts[0].txInfAndSts[0].stsRsnInf[0].rsn.cd")
            .value("AM04"));
  }

  @Test
  void idempotentReplayReturns200() throws Exception {
    when(ledgerRepository.settleTransfer(any(TransferCommand.class)))
        .thenReturn(
            TransferOutcome.replay(
                accepted("E2E-SC004-0001", DEBTOR, CREDITOR, 3000)));

    mockMvc
        .perform(
            post("/payment-initiations")
                .header(DemoAuthInterceptor.DEMO_USER_HEADER, "payer@demo")
                .contentType(APPLICATION_JSON)
                .content(pain001("E2E-SC004-0001", DEBTOR, CREDITOR, "30.00")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.orgnlPmtInfAndSts[0].txInfAndSts[0].txSts").value("ACSC"));
  }

  @Test
  void idempotencyConflictReturns409WithDu04() throws Exception {
    when(ledgerRepository.settleTransfer(any(TransferCommand.class)))
        .thenReturn(
            TransferOutcome.conflict(
                accepted("E2E-SC005-0001", DEBTOR, CREDITOR, 2000)));

    mockMvc
        .perform(
            post("/payment-initiations")
                .header(DemoAuthInterceptor.DEMO_USER_HEADER, "payer@demo")
                .contentType(APPLICATION_JSON)
                .content(pain001("E2E-SC005-0001", DEBTOR, OTHER_CREDITOR, "20.00")))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.orgnlPmtInfAndSts[0].txInfAndSts[0].txSts").value("RJCT"))
        .andExpect(jsonPath("$.orgnlPmtInfAndSts[0].txInfAndSts[0].stsRsnInf[0].rsn.cd")
            .value("DU04"));
  }

  @Test
  void selfTransferReturns201WithAg01() throws Exception {
    when(ledgerRepository.settleTransfer(any(TransferCommand.class)))
        .thenReturn(
            TransferOutcome.created(rejected("E2E-SC015-0001", "AG01", null)));

    mockMvc
        .perform(
            post("/payment-initiations")
                .header(DemoAuthInterceptor.DEMO_USER_HEADER, "payer@demo")
                .contentType(APPLICATION_JSON)
                .content(pain001("E2E-SC015-0001", DEBTOR, DEBTOR, "10.00")))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.orgnlPmtInfAndSts[0].txInfAndSts[0].stsRsnInf[0].rsn.cd")
            .value("AG01"));
  }

  @Test
  void zeroAmountReturns201WithAm12() throws Exception {
    when(ledgerRepository.settleTransfer(any(TransferCommand.class)))
        .thenReturn(
            TransferOutcome.created(rejected("E2E-SC014-0001", "AM12", null)));

    mockMvc
        .perform(
            post("/payment-initiations")
                .header(DemoAuthInterceptor.DEMO_USER_HEADER, "payer@demo")
                .contentType(APPLICATION_JSON)
                .content(pain001("E2E-SC014-0001", DEBTOR, CREDITOR, "0.00")))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.orgnlPmtInfAndSts[0].txInfAndSts[0].stsRsnInf[0].rsn.cd")
            .value("AM12"));
  }

  private static PaymentTransaction accepted(
      String endToEndId, UUID debtor, UUID creditor, long amountMinor) {
    return new PaymentTransaction(
        UUID.randomUUID(),
        endToEndId,
        Optional.empty(),
        debtor,
        creditor,
        new Money(amountMinor, "USD"),
        TransactionStatus.ACSC,
        List.of(),
        Instant.parse("2026-06-23T12:00:01Z"));
  }

  private static PaymentTransaction rejected(String endToEndId, String code, String detail) {
    List<String> info = detail == null ? List.of() : List.of(detail);
    return new PaymentTransaction(
        UUID.randomUUID(),
        endToEndId,
        Optional.empty(),
        DEBTOR,
        CREDITOR,
        new Money(5000, "USD"),
        TransactionStatus.RJCT,
        List.of(new StatusReason(code, info)),
        Instant.parse("2026-06-23T12:00:01Z"));
  }

  private static String pain001(
      String endToEndId, UUID debtorId, UUID creditorId, String amount) {
    return """
        {
          "grpHdr": {
            "msgId": "MSG-TEST-0001",
            "creDtTm": "2026-06-23T12:00:00Z",
            "nbOfTxs": "1",
            "ctrlSum": "%s",
            "initgPty": { "nm": "Acme Corp" }
          },
          "pmtInf": [{
            "pmtInfId": "PMT-TEST-0001",
            "pmtMtd": "TRF",
            "dbtr": { "nm": "Acme Corp" },
            "dbtrAcct": {
              "id": { "othr": { "id": "%s" } },
              "ccy": "USD"
            },
            "cdtTrfTxInf": [{
              "pmtId": { "endToEndId": "%s" },
              "amt": { "instdAmt": { "value": "%s", "ccy": "USD" } },
              "cdtr": { "nm": "Supplier Ltd" },
              "cdtrAcct": {
                "id": { "othr": { "id": "%s" } },
                "ccy": "USD"
              }
            }]
          }]
        }
        """
        .formatted(amount, debtorId, endToEndId, amount, creditorId);
  }
}
