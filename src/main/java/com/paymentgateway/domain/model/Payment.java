package com.paymentgateway.domain.model;

import java.util.UUID;

public record Payment(
        UUID id,
        String idempotencyKey,
        PaymentStatus status,
        String lastFour,
        int expiryMonth,
        int expiryYear,
        Money money,
        String authorizationCode) {

    public static Payment pending(UUID id, String idempotencyKey, String lastFour,
                                  int expiryMonth, int expiryYear, Money money) {
        return new Payment(id, idempotencyKey, PaymentStatus.PENDING, lastFour, expiryMonth, expiryYear, money, null);
    }

    public Payment authorize(String authorizationCode) {
        return new Payment(id, idempotencyKey, PaymentStatus.AUTHORIZED, lastFour, expiryMonth, expiryYear, money, authorizationCode);
    }

    public Payment decline() {
        return new Payment(id, idempotencyKey, PaymentStatus.DECLINED, lastFour, expiryMonth, expiryYear, money, null);
    }
}
