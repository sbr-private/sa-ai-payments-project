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
import com.payments.ledger.repository.LedgerRepository;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AccountControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private LedgerRepository ledgerRepository;

  @Test
  void registerAccountMatchesSc001() throws Exception {
    when(ledgerRepository.insertAccount(any(Account.class)))
        .thenAnswer(
            invocation -> {
              Account account = invocation.getArgument(0);
              return new Account(
                  account.getId(),
                  account.getOwner(),
                  account.getCcy(),
                  account.getBalance(),
                  account.getStatus(),
                  Instant.parse("2026-06-23T12:00:00Z"));
            });

    mockMvc
        .perform(
            post("/accounts")
                .header(DemoAuthInterceptor.DEMO_USER_HEADER, "benchmark@demo")
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                      "owner": {
                        "nm": "Acme Corp",
                        "id": { "othr": { "id": "user_123" } }
                      },
                      "ccy": "USD"
                    }
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.owner.nm").value("Acme Corp"))
        .andExpect(jsonPath("$.owner.id.othr.id").value("user_123"))
        .andExpect(jsonPath("$.ccy").value("USD"))
        .andExpect(jsonPath("$.bal.value").value("0.00"))
        .andExpect(jsonPath("$.bal.ccy").value("USD"))
        .andExpect(jsonPath("$.status").value("active"))
        .andExpect(jsonPath("$.creDtTm").value("2026-06-23T12:00:00Z"))
        .andExpect(jsonPath("$.id").isNotEmpty());
  }

  @Test
  void registerAccountRequiresDemoUserHeader() throws Exception {
    mockMvc
        .perform(
            post("/accounts")
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {"owner":{"nm":"Acme Corp"},"ccy":"USD"}
                    """))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void registerAccountRejectsInvalidCurrency() throws Exception {
    mockMvc
        .perform(
            post("/accounts")
                .header(DemoAuthInterceptor.DEMO_USER_HEADER, "benchmark@demo")
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {"owner":{"nm":"Acme Corp"},"ccy":"usd"}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
  }
}
