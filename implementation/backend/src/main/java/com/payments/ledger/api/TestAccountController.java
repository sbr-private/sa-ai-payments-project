package com.payments.ledger.api;

import com.payments.ledger.api.dto.TestCreditRequest;
import com.payments.ledger.domain.service.TestAccountService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnProperty(name = "ledger.test-helpers.enabled", havingValue = "true")
public class TestAccountController {

  private final TestAccountService testAccountService;

  public TestAccountController(TestAccountService testAccountService) {
    this.testAccountService = testAccountService;
  }

  @PostMapping("/test/accounts/{id}/credit")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void credit(@PathVariable UUID id, @Valid @RequestBody TestCreditRequest request) {
    testAccountService.credit(id, request);
  }

  @PostMapping("/test/accounts/{id}/close")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void close(@PathVariable UUID id) {
    testAccountService.close(id);
  }
}
