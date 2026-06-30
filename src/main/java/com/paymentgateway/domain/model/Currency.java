package com.paymentgateway.domain.model;

public record Currency(String code) {
    public static Currency of(String code) {
        if (code == null || !code.matches("[A-Z]{3}")) {
            throw new ValidationException("currency must be a 3-letter ISO 4217 code");
        }
        return new Currency(code);
    }
}
