package com.paymentgateway.domain.model;

import java.util.Set;

public record CurrencyAllowList(Set<String> allowed) {
    public CurrencyAllowList {
        if (allowed == null) {
            throw new ValidationException("currency.allowlist.required", "currency allow-list is required");
        }
        if (allowed.size() > 3) {
            throw new ValidationException("currency.allowlist.too_many", "currency allow-list must contain at most 3 currencies");
        }
        allowed = Set.copyOf(allowed);
    }

    public boolean contains(Currency currency) {
        return allowed.contains(currency.code());
    }
}
