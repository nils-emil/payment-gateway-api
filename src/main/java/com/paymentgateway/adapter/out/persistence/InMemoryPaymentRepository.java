package com.paymentgateway.adapter.out.persistence;

import com.paymentgateway.domain.model.Payment;
import com.paymentgateway.domain.port.out.PaymentRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryPaymentRepository implements PaymentRepository {

    private final ConcurrentHashMap<UUID, Payment> store = new ConcurrentHashMap<>();

    @Override
    public Payment save(Payment payment) {
        store.put(payment.id(), payment);
        return payment;
    }

    @Override
    public Optional<Payment> findById(UUID id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Optional<Payment> findByIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null) {
            return Optional.empty();
        }
        return store.values().stream()
                .filter(p -> idempotencyKey.equals(p.idempotencyKey()))
                .findFirst();
    }
}
