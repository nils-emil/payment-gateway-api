package com.paymentgateway.domain.usecase.processpayment.validation;

import com.paymentgateway.domain.model.ValidationError;
import com.paymentgateway.domain.port.in.PaymentCommand;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Order(40)
public class AmountValidationRule implements ValidationRule<PaymentCommand> {

    @Override
    public List<ValidationError> validate(PaymentCommand command) {
        Long amount = command.amount();
        if (amount == null) {
            return List.of(new ValidationError("amount.required", "amount is required"));
        }
        if (amount <= 0) {
            return List.of(new ValidationError("amount.invalid", "amount must be a positive integer in the minor currency unit"));
        }
        return List.of();
    }
}
