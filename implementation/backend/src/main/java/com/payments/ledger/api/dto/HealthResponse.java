package com.payments.ledger.api.dto;

public class HealthResponse {

    private final String status;

    public HealthResponse(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}
