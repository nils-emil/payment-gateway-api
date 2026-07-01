package com.paymentgateway.domain.support.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SensitiveTest {

    @Test
    void masksAllButLastFour() {
        assertEquals("****8877", Sensitive.maskCardNumber("2222405343248877"));
    }

    @Test
    void masksShortInput() {
        assertEquals("****", Sensitive.maskCardNumber("123"));
    }

    @Test
    void masksNullInput() {
        assertEquals("****", Sensitive.maskCardNumber(null));
    }

    @Test
    void lastFourAcrossNumberLengths() {
        assertEquals("8877", Sensitive.lastFour("2222405343248877"));
        assertEquals("1234", Sensitive.lastFour("12345678901234"));
        assertEquals("6789", Sensitive.lastFour("1234567890123456789"));
    }

    @Test
    void lastFourReturnsEmptyForShortOrNullInput() {
        assertEquals("", Sensitive.lastFour("123"));
        assertEquals("", Sensitive.lastFour(""));
        assertEquals("", Sensitive.lastFour(null));
    }
}
