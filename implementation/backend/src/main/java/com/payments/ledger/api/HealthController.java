package com.payments.ledger.api;

import com.payments.ledger.api.dto.HealthResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

  @GetMapping("/health")
  public HealthResponse health() {
    return new HealthResponse("ok");
  }

  /**
   * Readiness probe (SPEC §5 OP-006). Returns 503 until a database adapter is wired.
   */
  @GetMapping("/ready")
  public ResponseEntity<HealthResponse> ready() {
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body(new HealthResponse("database not configured"));
  }
}
