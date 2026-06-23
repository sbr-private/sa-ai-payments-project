package com.payments.ledger.api.dto;

import java.util.Map;

public class ErrorResponse {

  private final ErrorBody error;

  public ErrorResponse(ErrorBody error) {
    this.error = error;
  }

  public ErrorBody getError() {
    return error;
  }

  public static ErrorResponse of(String code, String message) {
    return new ErrorResponse(new ErrorBody(code, message, null));
  }

  public static ErrorResponse of(String code, String message, Map<String, Object> details) {
    return new ErrorResponse(new ErrorBody(code, message, details));
  }

  public static class ErrorBody {

    private final String code;
    private final String message;
    private final Map<String, Object> details;

    public ErrorBody(String code, String message, Map<String, Object> details) {
      this.code = code;
      this.message = message;
      this.details = details;
    }

    public String getCode() {
      return code;
    }

    public String getMessage() {
      return message;
    }

    public Map<String, Object> getDetails() {
      return details;
    }
  }
}
