package com.paymentgateway.domain.usecase.processpayment.validation;

import com.paymentgateway.domain.model.ValidationError;
import com.paymentgateway.domain.port.in.PaymentCommand;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Component
@Order(20)
public class ExpiryValidationRule implements ValidationRule<PaymentCommand> {

    private final Clock clock;

    public ExpiryValidationRule(Clock clock) {
        this.clock = clock;
    }

    @Override
    public List<ValidationError> validate(PaymentCommand command) {
        List<ValidationError> errors = new ArrayList<>();
        Integer month = command.expiryMonth();
        Integer year = command.expiryYear();
        if (month == null) {
            errors.add(new ValidationError("expiry.month.required", "expiry month is required"));
        } else if (month < 1 || month > 12) {
            errors.add(new ValidationError("expiry.month.invalid", "expiry month must be between 1 and 12"));
        }
        if (year == null) {
            errors.add(new ValidationError("expiry.year.required", "expiry year is required"));
        } else if (year < 1000 || year > 9999) {
            errors.add(new ValidationError("expiry.year.invalid", "expiry year must be a 4-digit year"));
        }
        if (errors.isEmpty() && YearMonth.of(year, month).isBefore(YearMonth.now(clock))) {
            errors.add(new ValidationError("expiry.past", "card expiry must be in the future"));
        }
        return errors;
    }
}
