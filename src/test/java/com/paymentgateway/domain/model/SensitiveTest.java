package com.paymentgateway.domain.model;

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
}
