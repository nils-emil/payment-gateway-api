package com.paymentgateway.domain.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MoneyTest {
    private final Currency gbp = Currency.of("GBP");

    @Test
    void acceptsPositiveAmount() {
        Money money = Money.of(gbp, 100);
        assertEquals(100, money.amount());
        assertEquals(gbp, money.currency());
    }

    @Test
    void rejectsZeroOrNegative() {
        assertThrows(ValidationException.class, () -> Money.of(gbp, 0));
        assertThrows(ValidationException.class, () -> Money.of(gbp, -5));
    }

    @Test
    void rejectsNullCurrency() {
        assertThrows(ValidationException.class, () -> Money.of(null, 100));
    }
}
