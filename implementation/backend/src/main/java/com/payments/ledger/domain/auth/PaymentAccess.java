package com.payments.ledger.domain.auth;

import com.payments.ledger.domain.exception.ForbiddenException;
import java.util.UUID;

public final class PaymentAccess {

  private PaymentAccess() {}

  public static void requireCanInitiate(DemoUser user, UUID debtorAccountId) {
    if (user.getRole() == UserRole.support) {
      throw new ForbiddenException("Support users cannot initiate payments");
    }
    if (user.getRole() == UserRole.payer && !user.getAccountIds().contains(debtorAccountId)) {
      throw new ForbiddenException("Payment must be initiated from an owned account");
    }
  }
}
