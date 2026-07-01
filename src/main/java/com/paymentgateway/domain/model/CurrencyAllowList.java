package com.paymentgateway.domain.model;

import com.paymentgateway.domain.support.exception.ValidationException;

import java.util.List;
import java.util.Set;

public record CurrencyAllowList(Set<String> allowed) {
    public CurrencyAllowList {
        if (allowed == null) {
            throw new ValidationException(List.of(new ValidationError("currency.allowlist.required", "currency allow-list is required")));
        }
        allowed = Set.copyOf(allowed);
    }

    public boolean contains(String code) {
        return allowed.contains(code);
    }
}
