package com.payments.ledger.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.payments.ledger.adapter.mongo.MongoLedgerRepository;
import com.payments.ledger.repository.LedgerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = "ledger.database=mongo")
class MongoAdapterConfigTest {

  @Autowired private LedgerRepository ledgerRepository;

  @Test
  void loadsMongoAdapterByDefault() {
    assertThat(ledgerRepository).isInstanceOf(MongoLedgerRepository.class);
  }
}
