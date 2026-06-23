package com.payments.ledger.domain.auth;

import com.payments.ledger.api.dto.LoginRequest;
import com.payments.ledger.api.dto.LoginResponse;
import com.payments.ledger.api.dto.UserResponse;
import com.payments.ledger.api.error.InvalidCredentialsException;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

  private final DemoUserRegistry userRegistry;

  public AuthService(DemoUserRegistry userRegistry) {
    this.userRegistry = userRegistry;
  }

  public LoginResponse login(LoginRequest request) {
    DemoUser user =
        userRegistry
            .findByEmail(request.getEmail())
            .filter(found -> found.getPassword().equals(request.getPassword()))
            .orElseThrow(InvalidCredentialsException::new);

    return new LoginResponse(toUserResponse(user));
  }

  public DemoUser requireUser(String email) {
    return userRegistry
        .findByEmail(email)
        .orElseThrow(() -> new UnauthorizedException("Unknown demo user"));
  }

  private static UserResponse toUserResponse(DemoUser user) {
    return new UserResponse(
        user.getEmail(),
        user.getDisplayName(),
        user.getRole(),
        user.getAccountIds().stream().map(UUID::toString).toList());
  }
}
