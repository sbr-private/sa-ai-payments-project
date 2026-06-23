package com.payments.ledger.adapter.mongo;

import com.payments.ledger.domain.model.IdempotencyFingerprint;
import com.payments.ledger.domain.model.Money;
import com.payments.ledger.domain.model.PaymentTransaction;
import com.payments.ledger.domain.model.StatusReason;
import com.payments.ledger.domain.model.TransactionStatus;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.bson.Document;

final class MongoPaymentTransactionMapper {

  private MongoPaymentTransactionMapper() {}

  static Document toDocument(PaymentTransaction transaction, IdempotencyFingerprint fingerprint) {
    Document pmtId = new Document("endToEndId", transaction.getEndToEndId());
    transaction.getInstrId().ifPresent(instrId -> pmtId.append("instrId", instrId));

    Document instdAmt =
        new Document("valueMinor", transaction.getInstructedAmount().getValueMinor())
            .append("ccy", transaction.getInstructedAmount().getCcy());

    Document document =
        new Document("_id", transaction.getTxId().toString())
            .append("pmtId", pmtId)
            .append("dbtrAcctId", transaction.getDebtorAccountId().toString())
            .append("cdtrAcctId", transaction.getCreditorAccountId().toString())
            .append("instdAmt", instdAmt)
            .append("txSts", transaction.getStatus().name())
            .append("idempotencyBody", toIdempotencyDocument(fingerprint))
            .append("creDtTm", Date.from(transaction.getCreatedAt()))
            .append("schemaVersion", 1);

    if (!transaction.getStatusReasons().isEmpty()) {
      document.append("stsRsnInf", toStatusReasonDocuments(transaction.getStatusReasons()));
    }

    return document;
  }

  static Document toIdempotencyDocument(IdempotencyFingerprint fingerprint) {
    return new Document("dbtrAcctId", fingerprint.getDebtorAccountId().toString())
        .append("cdtrAcctId", fingerprint.getCreditorAccountId().toString())
        .append(
            "instdAmt",
            new Document("valueMinor", fingerprint.getInstructedAmount().getValueMinor())
                .append("ccy", fingerprint.getInstructedAmount().getCcy()));
  }

  static PaymentTransaction toDomain(Document document) {
    Document pmtId = document.get("pmtId", Document.class);
    String endToEndId = pmtId.getString("endToEndId");
    Optional<String> instrId = Optional.ofNullable(pmtId.getString("instrId"));

    Document instdAmt = document.get("instdAmt", Document.class);
    Money amount =
        new Money(instdAmt.get("valueMinor", Number.class).longValue(), instdAmt.getString("ccy"));

    List<StatusReason> reasons = new ArrayList<>();
    @SuppressWarnings("unchecked")
    List<Document> stsRsnInf = (List<Document>) document.get("stsRsnInf");
    if (stsRsnInf != null) {
      for (Document reasonDoc : stsRsnInf) {
        Document rsn = reasonDoc.get("rsn", Document.class);
        @SuppressWarnings("unchecked")
        List<String> addtlInf = (List<String>) reasonDoc.get("addtlInf");
        reasons.add(
            new StatusReason(
                rsn.getString("cd"), addtlInf == null ? List.of() : List.copyOf(addtlInf)));
      }
    }

    Date createdAt = document.get("creDtTm", Date.class);

    return new PaymentTransaction(
        UUID.fromString(document.getString("_id")),
        endToEndId,
        instrId,
        UUID.fromString(document.getString("dbtrAcctId")),
        UUID.fromString(document.getString("cdtrAcctId")),
        amount,
        TransactionStatus.valueOf(document.getString("txSts")),
        reasons,
        createdAt.toInstant());
  }

  static IdempotencyFingerprint fingerprintFromDocument(Document document) {
    Document body = document.get("idempotencyBody", Document.class);
    Document instdAmt = body.get("instdAmt", Document.class);
    return new IdempotencyFingerprint(
        UUID.fromString(body.getString("dbtrAcctId")),
        UUID.fromString(body.getString("cdtrAcctId")),
        new Money(
            instdAmt.get("valueMinor", Number.class).longValue(), instdAmt.getString("ccy")));
  }

  private static List<Document> toStatusReasonDocuments(List<StatusReason> reasons) {
    List<Document> documents = new ArrayList<>();
    for (StatusReason reason : reasons) {
      Document doc =
          new Document("rsn", new Document("cd", reason.getCode()));
      if (!reason.getAdditionalInfo().isEmpty()) {
        doc.append("addtlInf", reason.getAdditionalInfo());
      }
      documents.add(doc);
    }
    return documents;
  }
}
