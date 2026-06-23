package com.payments.ledger.api.dto;

import com.payments.ledger.domain.auth.UserRole;
import java.util.List;

public class UserResponse {

  private final String email;
  private final String displayName;
  private final UserRole role;
  private final List<String> accountIds;

  public UserResponse(
      String email, String displayName, UserRole role, List<String> accountIds) {
    this.email = email;
    this.displayName = displayName;
    this.role = role;
    this.accountIds = List.copyOf(accountIds);
  }

  public String getEmail() {
    return email;
  }

  public String getDisplayName() {
    return displayName;
  }

  public UserRole getRole() {
    return role;
  }

  public List<String> getAccountIds() {
    return accountIds;
  }
}
