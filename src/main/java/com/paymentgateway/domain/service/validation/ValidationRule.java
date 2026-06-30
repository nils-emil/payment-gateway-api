package com.paymentgateway.domain.service.validation;

import com.paymentgateway.domain.model.ValidationError;

import java.util.List;

public interface ValidationRule<T> {

    List<ValidationError> validate(T target);
}
