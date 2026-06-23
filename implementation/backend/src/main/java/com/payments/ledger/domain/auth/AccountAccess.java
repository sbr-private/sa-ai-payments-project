package com.payments.ledger.domain.auth;

import com.payments.ledger.domain.exception.ForbiddenException;
import java.util.UUID;

public final class AccountAccess {

  private AccountAccess() {}

  public static void requireCanView(DemoUser user, UUID accountId) {
    if (user.getRole() == UserRole.payer && !user.getAccountIds().contains(accountId)) {
      throw new ForbiddenException("Account not accessible for this user");
    }
  }
}
