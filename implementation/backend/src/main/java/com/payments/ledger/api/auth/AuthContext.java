package com.payments.ledger.api.auth;

import com.payments.ledger.domain.auth.DemoUser;
import jakarta.servlet.http.HttpServletRequest;

public final class AuthContext {

  private AuthContext() {}

  public static DemoUser requireUser(HttpServletRequest request) {
    Object value = request.getAttribute(DemoAuthInterceptor.DEMO_USER_ATTRIBUTE);
    if (!(value instanceof DemoUser user)) {
      throw new IllegalStateException("Demo user not set on request");
    }
    return user;
  }
}
