package com.payments.ledger.domain.money;

import com.payments.ledger.domain.model.Money;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

public final class MoneyConverter {

  private static final Map<String, Integer> MINOR_UNITS =
      Map.of(
          "USD", 2,
          "EUR", 2,
          "GBP", 2,
          "JPY", 0,
          "BHD", 3);

  private MoneyConverter() {}

  public static int minorUnitsFor(String ccy) {
    return MINOR_UNITS.getOrDefault(ccy, 2);
  }

  public static long toMinorUnits(String decimalValue, String ccy) {
    int scale = minorUnitsFor(ccy);
    BigDecimal amount = new BigDecimal(decimalValue);
    if (amount.scale() > scale) {
      throw new IllegalArgumentException("Too many decimal places for " + ccy);
    }
    return amount.movePointRight(scale).setScale(0, RoundingMode.UNNECESSARY).longValueExact();
  }

  public static String toDecimalString(Money money) {
    int scale = minorUnitsFor(money.getCcy());
    BigDecimal amount =
        BigDecimal.valueOf(money.getValueMinor(), scale).stripTrailingZeros();
    if (scale == 0) {
      return amount.toPlainString();
    }
    return amount.setScale(scale, RoundingMode.UNNECESSARY).toPlainString();
  }

  public static Money zero(String ccy) {
    return new Money(0, ccy);
  }
}
