package com.paymentgateway.domain.service;

import com.paymentgateway.domain.model.Payment;
import com.paymentgateway.domain.model.PaymentNotFoundException;
import com.paymentgateway.domain.port.in.GetPaymentUseCase;
import com.paymentgateway.domain.port.out.PaymentRepository;

import java.util.UUID;

public class GetPaymentService implements GetPaymentUseCase {

    private final PaymentRepository repository;

    public GetPaymentService(PaymentRepository repository) {
        this.repository = repository;
    }

    @Override
    public Payment getById(UUID id) {
        return repository.findById(id).orElseThrow(() -> new PaymentNotFoundException(id));
    }
}
