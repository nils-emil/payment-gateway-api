package com.paymentgateway.domain.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CurrencyTest {
    @Test
    void acceptsThreeLetterUppercaseCode() {
        assertEquals("GBP", Currency.of("GBP").code());
    }

    @Test
    void rejectsNull() {
        ValidationException ex = assertThrows(ValidationException.class, () -> Currency.of(null));
        assertTrue(ex.errors().toString().toLowerCase().contains("currency"));
    }

    @Test
    void rejectsWrongLength() {
        assertThrows(ValidationException.class, () -> Currency.of("GB"));
        assertThrows(ValidationException.class, () -> Currency.of("GBPP"));
    }

    @Test
    void rejectsNonLetters() {
        assertThrows(ValidationException.class, () -> Currency.of("G1P"));
    }
}
