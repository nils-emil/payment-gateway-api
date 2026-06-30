package com.paymentgateway.domain.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PaymentTest {
    private final Money money = Money.of(Currency.of("GBP"), 100);
    private final ExpiryDate expiry = new ExpiryDate(4, 2027);

    @Test
    void pendingHasIdAndPendingStatus() {
        Payment p = Payment.pending("8877", expiry, money);
        assertNotNull(p.id());
        assertEquals(PaymentStatus.PENDING, p.status());
        assertNull(p.authorizationCode());
        assertNotNull(p.idempotencyKey());
    }

    @Test
    void authorizeKeepsIdSetsCodeAndStatus() {
        Payment p = Payment.pending("8877", expiry, money);
        Payment authorized = p.authorize("auth-123");
        assertEquals(p.id(), authorized.id());
        assertEquals(PaymentStatus.AUTHORIZED, authorized.status());
        assertEquals("auth-123", authorized.authorizationCode());
        assertEquals(p.idempotencyKey(), authorized.idempotencyKey());
    }

    @Test
    void declineKeepsIdSetsStatusNoCode() {
        Payment p = Payment.pending("8877", expiry, money);
        Payment declined = p.decline();
        assertEquals(p.id(), declined.id());
        assertEquals(PaymentStatus.DECLINED, declined.status());
        assertNull(declined.authorizationCode());
        assertEquals(p.idempotencyKey(), declined.idempotencyKey());
    }

    @Test
    void displayNamesAreTitleCase() {
        assertEquals("Authorized", PaymentStatus.AUTHORIZED.displayName());
        assertEquals("Declined", PaymentStatus.DECLINED.displayName());
        assertEquals("Pending", PaymentStatus.PENDING.displayName());
    }
}
