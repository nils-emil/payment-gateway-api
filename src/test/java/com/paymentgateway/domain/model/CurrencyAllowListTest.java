package com.paymentgateway.domain.model;

import com.paymentgateway.domain.support.exception.ValidationException;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CurrencyAllowListTest {

    @Test
    void checksMembership() {
        CurrencyAllowList allowList = new CurrencyAllowList(Set.of("GBP", "USD", "EUR"));
        assertTrue(allowList.contains("GBP"));
        assertFalse(allowList.contains("JPY"));
    }

    @Test
    void rejectsNull() {
        assertThrows(ValidationException.class, () -> new CurrencyAllowList(null));
    }
}
