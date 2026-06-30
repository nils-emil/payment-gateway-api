package com.paymentgateway.domain.model;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CurrencyAllowListTest {

    @Test
    void acceptsUpToThreeCurrenciesAndChecksMembership() {
        CurrencyAllowList allowList = new CurrencyAllowList(Set.of("GBP", "USD", "EUR"));
        assertTrue(allowList.contains(Currency.of("GBP")));
        assertFalse(allowList.contains(Currency.of("JPY")));
    }

    @Test
    void rejectsMoreThanThreeCurrencies() {
        assertThrows(ValidationException.class,
                () -> new CurrencyAllowList(Set.of("GBP", "USD", "EUR", "JPY")));
    }

    @Test
    void rejectsNull() {
        assertThrows(ValidationException.class, () -> new CurrencyAllowList(null));
    }
}
