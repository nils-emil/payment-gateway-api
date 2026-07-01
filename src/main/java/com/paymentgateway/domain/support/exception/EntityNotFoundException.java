package com.paymentgateway.domain.support.exception;

public class EntityNotFoundException extends RuntimeException {
    public EntityNotFoundException(String entity, Object id) {
        super(entity + " not found: " + id);
    }
}
