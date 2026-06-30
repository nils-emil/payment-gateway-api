package com.paymentgateway.domain.port.in;

public record PaymentRequest(
        String cardNumber,
        int expiryMonth,
        int expiryYear,
        String currency,
        long amount,
        String cvv) {
}
