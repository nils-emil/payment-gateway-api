package com.paymentgateway.domain.model;

import java.util.UUID;

public record Payment(
        UUID id,
        PaymentStatus status,
        String lastFour,
        ExpiryDate expiry,
        Money money,
        String authorizationCode) {

    public static Payment pending(String lastFour, ExpiryDate expiry, Money money) {
        return new Payment(UUID.randomUUID(), PaymentStatus.PENDING, lastFour, expiry, money, null);
    }

    public Payment authorize(String authorizationCode) {
        return new Payment(id, PaymentStatus.AUTHORIZED, lastFour, expiry, money, authorizationCode);
    }

    public Payment decline() {
        return new Payment(id, PaymentStatus.DECLINED, lastFour, expiry, money, null);
    }
}
