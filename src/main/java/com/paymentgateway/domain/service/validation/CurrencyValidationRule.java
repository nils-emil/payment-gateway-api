package com.paymentgateway.domain.service.validation;

import com.paymentgateway.domain.model.Currency;
import com.paymentgateway.domain.model.CurrencyAllowList;
import com.paymentgateway.domain.port.in.PaymentRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CurrencyValidationRule implements ValidationRule<PaymentRequest> {

    private final CurrencyAllowList allowList;

    public CurrencyValidationRule(CurrencyAllowList allowList) {
        this.allowList = allowList;
    }

    @Override
    public List<String> validate(PaymentRequest request) {
        String code = request.currency();
        if (code == null || !code.matches("[A-Z]{3}")) {
            return List.of("currency must be a 3-letter ISO 4217 code");
        }
        if (!allowList.contains(Currency.of(code))) {
            return List.of("currency " + code + " is not supported");
        }
        return List.of();
    }
}
