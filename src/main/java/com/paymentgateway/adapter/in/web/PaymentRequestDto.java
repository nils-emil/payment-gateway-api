package com.paymentgateway.adapter.in.web;

// JSON (global SNAKE_CASE): card_number, expiry_month, expiry_year, currency, amount, cvv
public record PaymentRequestDto(
        String cardNumber,
        int expiryMonth,
        int expiryYear,
        String currency,
        long amount,
        String cvv) {
}
