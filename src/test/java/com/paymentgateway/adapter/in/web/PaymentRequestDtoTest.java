package com.paymentgateway.adapter.in.web;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PaymentRequestDtoTest {

    @Test
    void toStringMasksCardNumberAndCvv() {
        PaymentRequestDto dto = new PaymentRequestDto("2222405343248877", 4, 2027, "GBP", 100L, "123");
        String text = dto.toString();
        assertTrue(text.contains("****8877"), text);
        assertFalse(text.contains("2222405343248877"), text);
        assertFalse(text.contains("123"), text);
    }
}
