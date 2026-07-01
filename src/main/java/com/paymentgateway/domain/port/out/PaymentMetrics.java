package com.paymentgateway.domain.port.out;

import com.paymentgateway.domain.model.Payment;

public interface PaymentMetrics {
    void recordOutcome(Payment payment);
}
