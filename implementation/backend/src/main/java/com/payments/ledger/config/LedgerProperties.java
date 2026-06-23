package com.payments.ledger.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ledger")
public class LedgerProperties {

  /** Active adapter: {@code mongo} or {@code postgres}. */
  private String database = "mongo";

  private final TestHelpers testHelpers = new TestHelpers();

  public String getDatabase() {
    return database;
  }

  public void setDatabase(String database) {
    this.database = database;
  }

  public TestHelpers getTestHelpers() {
    return testHelpers;
  }

  public static class TestHelpers {

    private boolean enabled = false;

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }
  }
}
