package com.paymentgateway.adapter.out.persistence;

import com.paymentgateway.domain.model.*;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryPaymentRepositoryTest {

    private final InMemoryPaymentRepository repository = new InMemoryPaymentRepository();
    private final Payment pending = Payment.pending(UUID.randomUUID(), "key-1", "8877", 4, 2027, new Money(100, "GBP"));

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
        assertTrue(repository.findById(UUID.randomUUID()).isEmpty());
    }

    @Test
    void findsByIdempotencyKey() {
        repository.save(pending);
        assertEquals(pending.id(), repository.findByIdempotencyKey("key-1").orElseThrow().id());
        assertTrue(repository.findByIdempotencyKey("missing").isEmpty());
    }

    @Test
    void findByNullIdempotencyKeyIsEmpty() {
        repository.save(pending);
        assertTrue(repository.findByIdempotencyKey(null).isEmpty());
    }
}
