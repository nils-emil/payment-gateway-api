package com.paymentgateway.domain.support.exception;

import com.paymentgateway.domain.model.ValidationError;

import java.util.List;
import java.util.stream.Collectors;

public class ValidationException extends RuntimeException {
    private final List<ValidationError> errors;

    public ValidationException(List<ValidationError> errors) {
        super(errors.stream().map(ValidationError::description).collect(Collectors.joining("; ")));
        this.errors = List.copyOf(errors);
    }

    public List<ValidationError> errors() {
        return errors;
    }
}
