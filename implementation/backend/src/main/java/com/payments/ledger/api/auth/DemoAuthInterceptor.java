package com.payments.ledger.api.auth;

import com.payments.ledger.domain.auth.AuthService;
import com.payments.ledger.domain.auth.DemoUser;
import com.payments.ledger.domain.auth.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class DemoAuthInterceptor implements HandlerInterceptor {

  public static final String DEMO_USER_ATTRIBUTE = "demoUser";
  public static final String DEMO_USER_HEADER = "X-Demo-User";

  private final AuthService authService;

  public DemoAuthInterceptor(AuthService authService) {
    this.authService = authService;
  }

  @Override
  public boolean preHandle(
      HttpServletRequest request, HttpServletResponse response, Object handler) {
    String email = request.getHeader(DEMO_USER_HEADER);
    if (email == null || email.isBlank()) {
      throw new UnauthorizedException("Missing X-Demo-User header");
    }

    DemoUser user = authService.requireUser(email.trim());
    request.setAttribute(DEMO_USER_ATTRIBUTE, user);
    return true;
  }
}
