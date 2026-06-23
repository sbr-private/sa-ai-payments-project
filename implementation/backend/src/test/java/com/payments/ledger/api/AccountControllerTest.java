package com.payments.ledger.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
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
import com.payments.ledger.repository.LedgerRepository;
import java.time.Instant;
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

  @Test
  void getAccountReturnsAccountForBenchmarkUser() throws Exception {
    UUID accountId = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
    when(ledgerRepository.findAccountById(accountId))
        .thenReturn(
            Optional.of(
                new Account(
                    accountId,
                    new Party("Acme Corp", Optional.of("user_123")),
                    "USD",
                    new Money(95000, "USD"),
                    AccountStatus.active,
                    Instant.parse("2026-06-23T12:00:00Z"))));

    mockMvc
        .perform(
            get("/accounts/{id}", accountId)
                .header(DemoAuthInterceptor.DEMO_USER_HEADER, "benchmark@demo"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(accountId.toString()))
        .andExpect(jsonPath("$.bal.value").value("950.00"));
  }

  @Test
  void getAccountReturnsNotFoundForMissingAccount() throws Exception {
    UUID missingId = UUID.fromString("00000000-0000-0000-0000-000000000000");
    when(ledgerRepository.findAccountById(missingId)).thenReturn(Optional.empty());

    mockMvc
        .perform(
            get("/accounts/{id}", missingId)
                .header(DemoAuthInterceptor.DEMO_USER_HEADER, "benchmark@demo"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error.code").value("NOT_FOUND"))
        .andExpect(jsonPath("$.error.details.resource").value("account"))
        .andExpect(jsonPath("$.error.details.id").value(missingId.toString()));
  }

  @Test
  void getAccountReturnsForbiddenWhenPayerViewsOtherAccount() throws Exception {
    UUID otherAccountId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    when(ledgerRepository.findAccountById(otherAccountId))
        .thenReturn(
            Optional.of(
                new Account(
                    otherAccountId,
                    new Party("Other", Optional.empty()),
                    "USD",
                    new Money(0, "USD"),
                    AccountStatus.active,
                    Instant.parse("2026-06-23T12:00:00Z"))));

    mockMvc
        .perform(
            get("/accounts/{id}", otherAccountId)
                .header(DemoAuthInterceptor.DEMO_USER_HEADER, "payer@demo"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
  }

  @Test
  void getAccountAllowsPayerToViewLinkedAccount() throws Exception {
    UUID payerAccountId = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
    when(ledgerRepository.findAccountById(payerAccountId))
        .thenReturn(
            Optional.of(
                new Account(
                    payerAccountId,
                    new Party("Acme Corp", Optional.empty()),
                    "USD",
                    new Money(95000, "USD"),
                    AccountStatus.active,
                    Instant.parse("2026-06-23T12:00:00Z"))));

    mockMvc
        .perform(
            get("/accounts/{id}", payerAccountId)
                .header(DemoAuthInterceptor.DEMO_USER_HEADER, "payer@demo"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(payerAccountId.toString()));
  }
}
