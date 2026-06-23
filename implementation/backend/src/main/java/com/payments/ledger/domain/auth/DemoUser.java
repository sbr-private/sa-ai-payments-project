package com.payments.ledger.domain.auth;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Hardcoded demo user — not stored in the ledger database. */
public final class DemoUser {

  private final String email;
  private final String password;
  private final String displayName;
  private final UserRole role;
  private final List<UUID> accountIds;

  public DemoUser(
      String email,
      String password,
      String displayName,
      UserRole role,
      List<UUID> accountIds) {
    this.email = Objects.requireNonNull(email, "email");
    this.password = Objects.requireNonNull(password, "password");
    this.displayName = Objects.requireNonNull(displayName, "displayName");
    this.role = Objects.requireNonNull(role, "role");
    this.accountIds = List.copyOf(accountIds);
  }

  public String getEmail() {
    return email;
  }

  public String getPassword() {
    return password;
  }

  public String getDisplayName() {
    return displayName;
  }

  public UserRole getRole() {
    return role;
  }

  public List<UUID> getAccountIds() {
    return accountIds;
  }
}
