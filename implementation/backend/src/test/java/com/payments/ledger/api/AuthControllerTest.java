package com.payments.ledger.api;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.payments.ledger.repository.LedgerRepository;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private LedgerRepository ledgerRepository;

  @Test
  void loginReturnsUserForValidCredentials() throws Exception {
    mockMvc
        .perform(
            post("/auth/login")
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {"email":"payer@demo","password":"demo"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.user.email").value("payer@demo"))
        .andExpect(jsonPath("$.user.displayName").value("Demo Payer"))
        .andExpect(jsonPath("$.user.role").value("payer"))
        .andExpect(jsonPath("$.user.accountIds[0]").value("a1b2c3d4-e5f6-7890-abcd-ef1234567890"));
  }

  @Test
  void loginReturnsInvalidCredentialsForWrongPassword() throws Exception {
    mockMvc
        .perform(
            post("/auth/login")
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {"email":"payer@demo","password":"wrong"}
                    """))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"));
  }

  @Test
  void loginReturnsInvalidCredentialsForUnknownEmail() throws Exception {
    mockMvc
        .perform(
            post("/auth/login")
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {"email":"nobody@demo","password":"demo"}
                    """))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"));
  }

  @Test
  void loginDoesNotRequireDemoUserHeader() throws Exception {
    mockMvc
        .perform(
            post("/auth/login")
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {"email":"benchmark@demo","password":"demo"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.user.role").value("benchmark"));
  }
}
