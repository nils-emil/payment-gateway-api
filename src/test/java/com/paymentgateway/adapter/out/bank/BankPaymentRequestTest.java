package com.paymentgateway.adapter.out.bank;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BankPaymentRequestTest {

    @Test
    void toStringMasksCardNumberAndCvv() {
        BankPaymentRequest request = new BankPaymentRequest("2222405343248877", "04/2027", "GBP", 100L, "123");
        String text = request.toString();
        assertTrue(text.contains("****8877"), text);
        assertFalse(text.contains("2222405343248877"), text);
        assertFalse(text.contains("123"), text);
    }
}
