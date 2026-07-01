package com.paymentgateway.domain.usecase.processpayment.validation;

import com.paymentgateway.domain.model.ValidationError;
import com.paymentgateway.domain.port.in.PaymentCommand;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Order(50)
public class CvvValidationRule implements ValidationRule<PaymentCommand> {

    @Override
    public List<ValidationError> validate(PaymentCommand command) {
        String cvv = command.cvv();
        if (cvv == null || !cvv.matches("\\d{3,4}")) {
            return List.of(new ValidationError("cvv.invalid", "cvv must be 3-4 digits"));
        }
        return List.of();
    }
}
