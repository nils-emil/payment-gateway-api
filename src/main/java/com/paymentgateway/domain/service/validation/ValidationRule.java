package com.paymentgateway.domain.service.validation;

import java.util.List;

public interface ValidationRule<T> {

    List<String> validate(T target);
}
