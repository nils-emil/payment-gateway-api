package com.paymentgateway.domain.usecase.processpayment.validation;

import com.paymentgateway.domain.model.ValidationError;

import java.util.List;

public interface ValidationRule<T> {

    List<ValidationError> validate(T target);
}
