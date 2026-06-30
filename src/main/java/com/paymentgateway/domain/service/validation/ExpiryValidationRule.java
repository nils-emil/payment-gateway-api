package com.paymentgateway.domain.service.validation;

import com.paymentgateway.domain.model.ValidationError;
import com.paymentgateway.domain.port.in.PaymentRequest;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Component
public class ExpiryValidationRule implements ValidationRule<PaymentRequest> {

    private final Clock clock;

    public ExpiryValidationRule(Clock clock) {
        this.clock = clock;
    }

    @Override
    public List<ValidationError> validate(PaymentRequest request) {
        List<ValidationError> errors = new ArrayList<>();
        int month = request.expiryMonth();
        int year = request.expiryYear();
        if (month < 1 || month > 12) {
            errors.add(new ValidationError("expiry.month.invalid", "expiry month must be between 1 and 12"));
        }
        if (year < 1 || year > 9999) {
            errors.add(new ValidationError("expiry.year.invalid", "expiry year must be between 1 and 9999"));
        }
        if (errors.isEmpty() && YearMonth.of(year, month).isBefore(YearMonth.now(clock))) {
            errors.add(new ValidationError("expiry.past", "card expiry must be in the future"));
        }
        return errors;
    }
}
