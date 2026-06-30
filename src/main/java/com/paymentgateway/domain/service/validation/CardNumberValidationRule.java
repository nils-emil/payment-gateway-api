package com.paymentgateway.domain.service.validation;

import com.paymentgateway.domain.model.ValidationError;
import com.paymentgateway.domain.port.in.PaymentRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CardNumberValidationRule implements ValidationRule<PaymentRequest> {

    @Override
    public List<ValidationError> validate(PaymentRequest request) {
        String number = request.cardNumber();
        if (number == null || !number.matches("\\d{14,19}")) {
            return List.of(new ValidationError("card.number.invalid", "card number must be 14-19 digits"));
        }
        return List.of();
    }
}
