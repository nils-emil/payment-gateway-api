package com.paymentgateway.domain.model;

public class BankUnavailableException extends RuntimeException {
    public BankUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
