package com.payments.ledger.adapter.mongo;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "ledger.database", havingValue = "mongo", matchIfMissing = true)
class MongoIndexInitializer {

  private final MongoTemplate mongoTemplate;

  MongoIndexInitializer(MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }

  @PostConstruct
  void ensureIndexes() {
    mongoTemplate
        .indexOps(MongoCollections.PAYMENT_TRANSACTIONS)
        .ensureIndex(new Index().on("pmtId.endToEndId", Sort.Direction.ASC).unique());

    mongoTemplate
        .indexOps(MongoCollections.STATEMENT_ENTRIES)
        .ensureIndex(
            new Index()
                .on("acctId", Sort.Direction.ASC)
                .on("creDtTm", Sort.Direction.DESC)
                .on("_id", Sort.Direction.DESC));
  }
}
