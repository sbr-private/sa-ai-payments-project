package com.payments.ledger.adapter.mongo;

import static org.assertj.core.api.Assertions.assertThat;

import com.payments.ledger.domain.model.Account;
import com.payments.ledger.domain.model.AccountStatus;
import com.payments.ledger.domain.model.Money;
import com.payments.ledger.domain.model.Party;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.bson.Document;
import org.junit.jupiter.api.Test;

class MongoAccountMapperTest {

  @Test
  void roundTripsAccountDocument() {
    UUID id = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
    Instant createdAt = Instant.parse("2026-06-23T12:00:00Z");
    Account account =
        new Account(
            id,
            new Party("Acme Corp", Optional.of("user_123")),
            "USD",
            new Money(0, "USD"),
            AccountStatus.active,
            createdAt);

    Document document = MongoAccountMapper.toDocument(account);
    Account restored = MongoAccountMapper.toDomain(document);

    assertThat(restored.getId()).isEqualTo(id);
    assertThat(restored.getOwner().getName()).isEqualTo("Acme Corp");
    assertThat(restored.getOwner().getExternalId()).contains("user_123");
    assertThat(restored.getCcy()).isEqualTo("USD");
    assertThat(restored.getBalance().getValueMinor()).isZero();
    assertThat(restored.getStatus()).isEqualTo(AccountStatus.active);
    assertThat(restored.getCreatedAt()).isEqualTo(createdAt);
  }
}
