package com.payments.ledger.api;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.payments.ledger.repository.LedgerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class HealthControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private LedgerRepository ledgerRepository;

  @Test
  void healthReturnsOk() throws Exception {
    mockMvc
        .perform(get("/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ok"));
  }

  @Test
  void readyReturnsOkWhenDatabaseIsHealthy() throws Exception {
    when(ledgerRepository.isHealthy()).thenReturn(true);

    mockMvc
        .perform(get("/ready"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ok"));
  }

  @Test
  void readyReturnsServiceUnavailableWhenDatabaseIsDown() throws Exception {
    when(ledgerRepository.isHealthy()).thenReturn(false);

    mockMvc
        .perform(get("/ready"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.status").value("database unavailable"));
  }
}
