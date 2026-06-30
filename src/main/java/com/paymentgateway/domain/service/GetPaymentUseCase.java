package com.paymentgateway.domain.service;

import com.paymentgateway.domain.model.Payment;
import com.paymentgateway.domain.model.PaymentNotFoundException;
import com.paymentgateway.domain.port.out.PaymentRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class GetPaymentUseCase {

    private final PaymentRepository repository;

    public GetPaymentUseCase(PaymentRepository repository) {
        this.repository = repository;
    }

    public Payment getById(UUID id) {
        return repository.findById(id).orElseThrow(() -> new PaymentNotFoundException(id));
    }
}
