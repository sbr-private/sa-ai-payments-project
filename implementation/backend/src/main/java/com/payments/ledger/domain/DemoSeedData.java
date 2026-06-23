package com.payments.ledger.domain;

import java.util.UUID;

/** Fixed identifiers for minimal demo seed — see docs/SEED.md. */
public final class DemoSeedData {

  public static final UUID ACME_ACCOUNT_ID =
      UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
  public static final UUID SUPPLIER_ACCOUNT_ID =
      UUID.fromString("b2c3d4e5-f6a7-8901-bcde-f12345678901");

  public static final String E2E_OPENING = "E2E-SEED-0001";
  public static final String E2E_SUCCESS = "E2E-INV-2024-0558";
  public static final String E2E_FAILED = "E2E-INV-2024-0999";

  public static final String INSTR_SUCCESS = "INSTR-20260623-0001";
  public static final String INSTR_FAILED = "INSTR-20260623-0002";

  private DemoSeedData() {}
}
