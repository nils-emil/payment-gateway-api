package com.paymentgateway.domain.model;

import java.util.Set;

public record CurrencyAllowList(Set<String> allowed) {
    public CurrencyAllowList {
        allowed = Set.copyOf(allowed);
    }

    public boolean contains(Currency currency) {
        return allowed.contains(currency.code());
    }
}
