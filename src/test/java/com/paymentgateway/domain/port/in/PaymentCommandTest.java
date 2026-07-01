package com.paymentgateway.domain.port.in;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PaymentCommandTest {

    @Test
    void toStringMasksCardNumberAndCvv() {
        PaymentCommand request = new PaymentCommand("2222405343248877", 4, 2027, "GBP", 100L, "123", null);
        String text = request.toString();
        assertTrue(text.contains("****8877"), text);
        assertFalse(text.contains("2222405343248877"), text);
        assertFalse(text.contains("123"), text);
    }
}
