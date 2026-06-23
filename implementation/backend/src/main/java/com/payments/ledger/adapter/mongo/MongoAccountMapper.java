package com.payments.ledger.adapter.mongo;

import com.payments.ledger.domain.model.Account;
import com.payments.ledger.domain.model.AccountStatus;
import com.payments.ledger.domain.model.Money;
import com.payments.ledger.domain.model.Party;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import org.bson.Document;

final class MongoAccountMapper {

  private MongoAccountMapper() {}

  static Document toDocument(Account account) {
    Document owner = new Document("nm", account.getOwner().getName());
    account
        .getOwner()
        .getExternalId()
        .ifPresent(
            externalId ->
                owner.append("id", new Document("othr", new Document("id", externalId))));

    Document balance =
        new Document("valueMinor", account.getBalance().getValueMinor())
            .append("ccy", account.getBalance().getCcy());

    return new Document("_id", account.getId().toString())
        .append("owner", owner)
        .append("ccy", account.getCcy())
        .append("bal", balance)
        .append("status", account.getStatus().name())
        .append("creDtTm", Date.from(account.getCreatedAt()))
        .append("schemaVersion", 1);
  }

  static Account toDomain(Document document) {
    Document ownerDoc = document.get("owner", Document.class);
    String name = ownerDoc.getString("nm");
    Optional<String> externalId = Optional.empty();
    Document idDoc = ownerDoc.get("id", Document.class);
    if (idDoc != null) {
      Document othr = idDoc.get("othr", Document.class);
      if (othr != null && othr.getString("id") != null) {
        externalId = Optional.of(othr.getString("id"));
      }
    }

    Document balDoc = document.get("bal", Document.class);
    Money balance =
        new Money(balDoc.get("valueMinor", Number.class).longValue(), balDoc.getString("ccy"));

    Date createdAt = document.get("creDtTm", Date.class);

    return new Account(
        UUID.fromString(document.getString("_id")),
        new Party(name, externalId),
        document.getString("ccy"),
        balance,
        AccountStatus.valueOf(document.getString("status")),
        createdAt.toInstant());
  }
}
