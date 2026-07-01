package com.paymentgateway.adapter.in.web;

import java.util.UUID;

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
