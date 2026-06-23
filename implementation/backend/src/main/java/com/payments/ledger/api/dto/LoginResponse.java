package com.payments.ledger.api.dto;

public class LoginResponse {

  private final UserResponse user;

  public LoginResponse(UserResponse user) {
    this.user = user;
  }

  public UserResponse getUser() {
    return user;
  }
}
