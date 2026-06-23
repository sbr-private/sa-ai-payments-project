package com.payments.ledger.adapter.mongo;

import com.payments.ledger.domain.model.CreditDebitIndicator;
import com.payments.ledger.domain.model.Money;
import com.payments.ledger.domain.model.StatementEntry;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
import java.util.UUID;
import org.bson.Document;

final class MongoStatementEntryMapper {

  private MongoStatementEntryMapper() {}

  static Document toDocument(StatementEntry entry) {
    String balanceIndicator = entry.getBalanceAfter().getValueMinor() >= 0 ? "CRDT" : "DBIT";

    return new Document("_id", entry.getEntryRef().toString())
        .append("acctId", entry.getAccountId().toString())
        .append("txId", entry.getTxId().toString())
        .append("endToEndId", entry.getEndToEndId())
        .append(
            "amt",
            new Document("valueMinor", entry.getAmount().getValueMinor())
                .append("ccy", entry.getAmount().getCcy()))
        .append("cdtDbtInd", entry.getCreditDebitIndicator().name())
        .append(
            "bal",
            new Document("valueMinor", Math.abs(entry.getBalanceAfter().getValueMinor()))
                .append("ccy", entry.getBalanceAfter().getCcy())
                .append("cdtDbtInd", balanceIndicator))
        .append("bookgDt", entry.getBookingDate().toString())
        .append("sts", "BOOK")
        .append("creDtTm", Date.from(entry.getCreatedAt()))
        .append("schemaVersion", 1);
  }

  static StatementEntry toDomain(Document document) {
    Document amt = document.get("amt", Document.class);
    Document bal = document.get("bal", Document.class);
    long balanceMinor = bal.get("valueMinor", Number.class).longValue();
    if ("DBIT".equals(bal.getString("cdtDbtInd"))) {
      balanceMinor = -balanceMinor;
    }

    Date createdAt = document.get("creDtTm", Date.class);

    return new StatementEntry(
        UUID.fromString(document.getString("_id")),
        UUID.fromString(document.getString("acctId")),
        UUID.fromString(document.getString("txId")),
        document.getString("endToEndId"),
        new Money(amt.get("valueMinor", Number.class).longValue(), amt.getString("ccy")),
        CreditDebitIndicator.valueOf(document.getString("cdtDbtInd")),
        new Money(balanceMinor, bal.getString("ccy")),
        LocalDate.parse(document.getString("bookgDt")),
        createdAt.toInstant());
  }
}
