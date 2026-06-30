package com.paymentgateway.domain.service.validation;

import com.paymentgateway.domain.model.ValidationError;
import com.paymentgateway.domain.port.in.PaymentRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AmountValidationRule implements ValidationRule<PaymentRequest> {

    @Override
    public List<ValidationError> validate(PaymentRequest request) {
        if (request.amount() <= 0) {
            return List.of(new ValidationError("amount.invalid", "amount must be a positive integer in the minor currency unit"));
        }
        return List.of();
    }
}
