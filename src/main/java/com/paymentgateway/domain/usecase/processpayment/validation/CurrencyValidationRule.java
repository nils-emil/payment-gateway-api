package com.paymentgateway.domain.usecase.processpayment.validation;

import com.paymentgateway.domain.model.CurrencyAllowList;
import com.paymentgateway.domain.model.ValidationError;
import com.paymentgateway.domain.port.in.PaymentCommand;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Order(30)
public class CurrencyValidationRule implements ValidationRule<PaymentCommand> {

    private static final String ISO_4217_PATTERN = "[A-Z]{3}";

    private final CurrencyAllowList allowList;

    public CurrencyValidationRule(CurrencyAllowList allowList) {
        this.allowList = allowList;
    }

    @Override
    public List<ValidationError> validate(PaymentCommand command) {
        String code = command.currency();
        if (code == null || !code.matches(ISO_4217_PATTERN)) {
            return List.of(new ValidationError("currency.invalid", "currency must be a 3-letter ISO 4217 code"));
        }
        if (!allowList.contains(code)) {
            return List.of(new ValidationError("currency.unsupported", "currency " + code + " is not supported"));
        }
        return List.of();
    }
}
