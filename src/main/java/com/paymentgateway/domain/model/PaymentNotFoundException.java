package com.paymentgateway.domain.model;

import java.util.UUID;

public class PaymentNotFoundException extends RuntimeException {
    public PaymentNotFoundException(UUID id) {
        super("payment not found: " + id);
    }
}
