package com.payments.ledger.api;

import com.payments.ledger.api.auth.AuthContext;
import com.payments.ledger.api.dto.statement.AccountStatementResponse;
import com.payments.ledger.domain.service.StatementService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.Optional;
import java.util.UUID;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
public class StatementController {

  private final StatementService statementService;

  public StatementController(StatementService statementService) {
    this.statementService = statementService;
  }

  @GetMapping("/accounts/{id}/statements")
  public AccountStatementResponse getStatement(
      @PathVariable UUID id,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
      @RequestParam(required = false) String cursor,
      HttpServletRequest request) {
    return statementService.getStatement(
        id,
        AuthContext.requireUser(request),
        limit,
        Optional.ofNullable(cursor));
  }
}
