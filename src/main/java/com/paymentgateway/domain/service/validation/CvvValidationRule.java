package com.paymentgateway.domain.service.validation;

import com.paymentgateway.domain.model.ValidationError;
import com.paymentgateway.domain.port.in.PaymentRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CvvValidationRule implements ValidationRule<PaymentRequest> {

    @Override
    public List<ValidationError> validate(PaymentRequest request) {
        String cvv = request.cvv();
        if (cvv == null || !cvv.matches("\\d{3,4}")) {
            return List.of(new ValidationError("cvv.invalid", "cvv must be 3-4 digits"));
        }
        return List.of();
    }
}
