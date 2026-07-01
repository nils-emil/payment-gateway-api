package com.paymentgateway.domain.usecase.processpayment.validation;

import com.paymentgateway.domain.model.ValidationError;
import com.paymentgateway.domain.port.in.PaymentCommand;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Order(10)
public class CardNumberValidationRule implements ValidationRule<PaymentCommand> {

    @Override
    public List<ValidationError> validate(PaymentCommand command) {
        String number = command.cardNumber();
        if (number == null || !number.matches("\\d{14,19}")) {
            return List.of(new ValidationError("card.number.invalid", "card number must be 14-19 digits"));
        }
        return List.of();
    }
}
