package com.paymentgateway.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PaymentTest {

    private static final Money MONEY = new Money(100, "GBP");

    private Payment pending() {
        return Payment.pending(UUID.randomUUID(), null, "8877", 4, 2027, MONEY);
    }

    @Test
    void pendingHasIdAndPendingStatus() {
        Payment p = pending();
        assertNotNull(p.id());
        assertEquals(PaymentStatus.PENDING, p.status());
        assertNull(p.authorizationCode());
    }

    @Test
    void authorizeKeepsIdSetsCodeAndStatus() {
        Payment p = pending();
        Payment authorized = p.authorize("auth-123");
        assertEquals(p.id(), authorized.id());
        assertEquals(PaymentStatus.AUTHORIZED, authorized.status());
        assertEquals("auth-123", authorized.authorizationCode());
    }

    @Test
    void declineKeepsIdSetsStatusNoCode() {
        Payment p = pending();
        Payment declined = p.decline();
        assertEquals(p.id(), declined.id());
        assertEquals(PaymentStatus.DECLINED, declined.status());
        assertNull(declined.authorizationCode());
    }

    @Test
    void displayNamesAreTitleCase() {
        assertEquals("Authorized", PaymentStatus.AUTHORIZED.displayName());
        assertEquals("Declined", PaymentStatus.DECLINED.displayName());
        assertEquals("Pending", PaymentStatus.PENDING.displayName());
    }
}
