package com.paymentgateway.domain.model;

public record Money(Currency currency, long amount) {
    public Money {
        if (currency == null) {
            throw new ValidationException("currency is required");
        }
        if (amount <= 0) {
            throw new ValidationException("amount must be a positive integer in the minor currency unit");
        }
    }

    public static Money of(Currency currency, long amount) {
        return new Money(currency, amount);
    }
}
