package com.paymentgateway.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MoneyTest {

    @Test
    void holdsAmountAndCurrency() {
        Money money = new Money(100, "GBP");
        assertEquals(100, money.amount());
        assertEquals("GBP", money.currency());
    }

    @Test
    void rejectsNonPositiveAmount() {
        assertThrows(IllegalArgumentException.class, () -> new Money(0, "GBP"));
        assertThrows(IllegalArgumentException.class, () -> new Money(-1, "GBP"));
    }

    @Test
    void rejectsMalformedCurrency() {
        assertThrows(IllegalArgumentException.class, () -> new Money(100, null));
        assertThrows(IllegalArgumentException.class, () -> new Money(100, "gbp"));
        assertThrows(IllegalArgumentException.class, () -> new Money(100, "POUND"));
    }
}
