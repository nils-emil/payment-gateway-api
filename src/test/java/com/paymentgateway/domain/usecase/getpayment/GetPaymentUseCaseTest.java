package com.paymentgateway.domain.usecase.getpayment;

import com.paymentgateway.domain.support.exception.*;
import com.paymentgateway.domain.model.*;
import com.paymentgateway.domain.port.out.PaymentRepository;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class GetPaymentUseCaseTest {

    private final Map<UUID, Payment> store = new HashMap<>();
    private final PaymentRepository repository = new PaymentRepository() {
        public Payment save(Payment p) { store.put(p.id(), p); return p; }
        public Optional<Payment> findById(UUID id) { return Optional.ofNullable(store.get(id)); }
        public Optional<Payment> findByIdempotencyKey(String key) {
            return store.values().stream().filter(p -> key.equals(p.idempotencyKey())).findFirst();
        }
    };

    @Test
    void returnsStoredPayment() {
        Payment p = Payment.pending(UUID.randomUUID(), null, "8877", 4, 2027, new Money(100, "GBP"));
        repository.save(p);
        var service = new GetPaymentUseCase(repository);
        assertEquals(p.id(), service.getById(p.id()).id());
    }

    @Test
    void throwsWhenMissing() {
        var service = new GetPaymentUseCase(repository);
        assertThrows(EntityNotFoundException.class, () -> service.getById(UUID.randomUUID()));
    }
}
