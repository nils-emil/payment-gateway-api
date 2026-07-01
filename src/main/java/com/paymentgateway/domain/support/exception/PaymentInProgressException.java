package com.paymentgateway.domain.support.exception;

import java.util.UUID;

public class PaymentInProgressException extends RuntimeException {
    public PaymentInProgressException(UUID paymentId) {
        super("payment " + paymentId + " is still being processed");
    }
}
