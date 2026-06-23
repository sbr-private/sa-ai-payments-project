package com.payments.ledger.config;

import com.payments.ledger.adapter.mongo.MongoLedgerRepository;
import com.payments.ledger.repository.LedgerRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

@Configuration
@ConditionalOnProperty(name = "ledger.database", havingValue = "mongo", matchIfMissing = true)
public class MongoAdapterConfig {

  @Bean
  public LedgerRepository ledgerRepository(MongoTemplate mongoTemplate) {
    return new MongoLedgerRepository(mongoTemplate);
  }
}
