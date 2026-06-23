package com.payments.ledger.domain.money;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.payments.ledger.domain.model.Money;
import org.junit.jupiter.api.Test;

class MoneyConverterTest {

  @Test
  void formatsZeroUsdBalance() {
    assertThat(MoneyConverter.toDecimalString(new Money(0, "USD"))).isEqualTo("0.00");
  }

  @Test
  void formatsUsdAmountWithTwoDecimalPlaces() {
    assertThat(MoneyConverter.toDecimalString(new Money(5000, "USD"))).isEqualTo("50.00");
  }

  @Test
  void parsesUsdDecimalToMinorUnits() {
    assertThat(MoneyConverter.toMinorUnits("50.00", "USD")).isEqualTo(5000L);
  }

  @Test
  void rejectsExcessDecimalPlacesForUsd() {
    assertThatThrownBy(() -> MoneyConverter.toMinorUnits("10.001", "USD"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
