package com.payments.ledger.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.payments.ledger.adapter.postgres.PostgresLedgerRepository;
import com.payments.ledger.repository.LedgerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(
    properties = {
      "ledger.database=postgres",
      "spring.autoconfigure.exclude="
          + "org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration,"
          + "org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration"
    })
class PostgresAdapterConfigTest {

  @Autowired private LedgerRepository ledgerRepository;

  @Test
  void loadsPostgresAdapterWhenConfigured() {
    assertThat(ledgerRepository).isInstanceOf(PostgresLedgerRepository.class);
  }
}
