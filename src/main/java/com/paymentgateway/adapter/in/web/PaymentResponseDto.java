package com.paymentgateway.adapter.in.web;

import java.util.UUID;

// JSON (global SNAKE_CASE, nulls omitted): id, status, last_four, expiry_month,
// expiry_year, currency, amount, authorization_code
public record PaymentResponseDto(
        UUID id,
        String status,
        String lastFour,
        int expiryMonth,
        int expiryYear,
        String currency,
        long amount,
        String authorizationCode) {
}
