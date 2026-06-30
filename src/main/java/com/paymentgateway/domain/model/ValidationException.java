package com.paymentgateway.domain.model;

import java.util.List;
import java.util.stream.Collectors;

public class ValidationException extends RuntimeException {
    private final List<ValidationError> errors;

    public ValidationException(String code, String description) {
        this(new ValidationError(code, description));
    }

    public ValidationException(ValidationError error) {
        this(List.of(error));
    }

    public ValidationException(List<ValidationError> errors) {
        super(errors.stream().map(ValidationError::description).collect(Collectors.joining("; ")));
        this.errors = List.copyOf(errors);
    }

    public List<ValidationError> errors() {
        return errors;
    }
}
