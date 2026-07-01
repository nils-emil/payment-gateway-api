package com.paymentgateway.domain.support.exception;

public class BankUnavailableException extends RuntimeException {

    public BankUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
