package com.paymentgateway.adapter.out.persistence;

import com.paymentgateway.domain.model.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryPaymentRepositoryTest {

    private final InMemoryPaymentRepository repository = new InMemoryPaymentRepository();
    private final Payment pending = Payment.pending("8877", new ExpiryDate(4, 2027), Money.of(Currency.of("GBP"), 100));

    @Test
    void savesAndFindsById() {
        repository.save(pending);
        assertTrue(repository.findById(pending.id()).isPresent());
    }

    @Test
    void saveUpsertsById() {
        repository.save(pending);
        repository.save(pending.authorize("auth-1"));
        assertEquals(PaymentStatus.AUTHORIZED, repository.findById(pending.id()).orElseThrow().status());
    }

    @Test
    void findByUnknownIdIsEmpty() {
        assertTrue(repository.findById(java.util.UUID.randomUUID()).isEmpty());
    }
}
