package com.payments.ledger.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.payments.ledger.api.auth.DemoAuthInterceptor;
import com.payments.ledger.domain.model.Account;
import com.payments.ledger.domain.model.AccountStatus;
import com.payments.ledger.domain.model.Money;
import com.payments.ledger.domain.model.Party;
import com.payments.ledger.domain.model.PaymentTransaction;
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
class PaymentInitiationControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private LedgerRepository ledgerRepository;

  @Test
  void initiatePaymentReturns201ForAcsc() throws Exception {
    UUID debtor = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
    UUID creditor = UUID.fromString("b2c3d4e5-f6a7-8901-bcde-f12345678901");

    PaymentTransaction transaction =
        new PaymentTransaction(
            UUID.randomUUID(),
            "E2E-SC002-0001",
            Optional.of("INSTR-20260623-0001"),
            debtor,
            creditor,
            new Money(5000, "USD"),
            TransactionStatus.ACSC,
            List.of(),
            Instant.parse("2026-06-23T12:00:01Z"));

    when(ledgerRepository.settleTransfer(any(TransferCommand.class)))
        .thenReturn(TransferOutcome.created(transaction));

    mockMvc
        .perform(
            post("/payment-initiations")
                .header(DemoAuthInterceptor.DEMO_USER_HEADER, "payer@demo")
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                      "grpHdr": {
                        "msgId": "MSG-20260623-0001",
                        "creDtTm": "2026-06-23T12:00:00Z",
                        "nbOfTxs": "1",
                        "ctrlSum": "50.00",
                        "initgPty": { "nm": "Acme Corp" }
                      },
                      "pmtInf": [{
                        "pmtInfId": "PMT-20260623-0001",
                        "pmtMtd": "TRF",
                        "dbtr": { "nm": "Acme Corp" },
                        "dbtrAcct": {
                          "id": { "othr": { "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890" } },
                          "ccy": "USD"
                        },
                        "cdtTrfTxInf": [{
                          "pmtId": {
                            "instrId": "INSTR-20260623-0001",
                            "endToEndId": "E2E-SC002-0001"
                          },
                          "amt": { "instdAmt": { "value": "50.00", "ccy": "USD" } },
                          "cdtr": { "nm": "Supplier Ltd" },
                          "cdtrAcct": {
                            "id": { "othr": { "id": "b2c3d4e5-f6a7-8901-bcde-f12345678901" } },
                            "ccy": "USD"
                          }
                        }]
                      }]
                    }
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.orgnlGrpInfAndSts.grpSts").value("ACSC"))
        .andExpect(jsonPath("$.orgnlPmtInfAndSts[0].txInfAndSts[0].txSts").value("ACSC"))
        .andExpect(jsonPath("$.orgnlPmtInfAndSts[0].txInfAndSts[0].orgnlEndToEndId")
            .value("E2E-SC002-0001"));
  }

  @Test
  void initiatePaymentForbiddenForSupportUser() throws Exception {
    mockMvc
        .perform(
            post("/payment-initiations")
                .header(DemoAuthInterceptor.DEMO_USER_HEADER, "support@demo")
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                      "grpHdr": {
                        "msgId": "MSG-1",
                        "creDtTm": "2026-06-23T12:00:00Z",
                        "nbOfTxs": "1",
                        "ctrlSum": "50.00",
                        "initgPty": { "nm": "Acme Corp" }
                      },
                      "pmtInf": [{
                        "pmtInfId": "PMT-1",
                        "pmtMtd": "TRF",
                        "dbtr": { "nm": "Acme Corp" },
                        "dbtrAcct": {
                          "id": { "othr": { "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890" } },
                          "ccy": "USD"
                        },
                        "cdtTrfTxInf": [{
                          "pmtId": { "endToEndId": "E2E-1" },
                          "amt": { "instdAmt": { "value": "50.00", "ccy": "USD" } },
                          "cdtr": { "nm": "Supplier Ltd" },
                          "cdtrAcct": {
                            "id": { "othr": { "id": "b2c3d4e5-f6a7-8901-bcde-f12345678901" } },
                            "ccy": "USD"
                          }
                        }]
                      }]
                    }
                    """))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
  }
}
