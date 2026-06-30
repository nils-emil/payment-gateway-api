package com.paymentgateway.domain.service;

import com.paymentgateway.domain.model.*;
import com.paymentgateway.domain.port.out.PaymentRepository;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class GetPaymentServiceTest {

    private final Map<UUID, Payment> store = new HashMap<>();
    private final PaymentRepository repository = new PaymentRepository() {
        public Payment save(Payment p) { store.put(p.id(), p); return p; }
        public Optional<Payment> findById(UUID id) { return Optional.ofNullable(store.get(id)); }
    };

    @Test
    void returnsStoredPayment() {
        Payment p = Payment.pending("8877", new ExpiryDate(4, 2027), Money.of(Currency.of("GBP"), 100));
        repository.save(p);
        var service = new GetPaymentService(repository);
        assertEquals(p.id(), service.getById(p.id()).id());
    }

    @Test
    void throwsWhenMissing() {
        var service = new GetPaymentService(repository);
        assertThrows(PaymentNotFoundException.class, () -> service.getById(UUID.randomUUID()));
    }
}
