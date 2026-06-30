package com.paymentgateway.domain.port.in;

import com.paymentgateway.domain.model.Payment;

public interface ProcessPaymentUseCase {
    Payment process(PaymentRequest request);
}
