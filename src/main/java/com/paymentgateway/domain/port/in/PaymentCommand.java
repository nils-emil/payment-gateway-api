package com.paymentgateway.domain.port.in;

import com.paymentgateway.domain.support.util.Sensitive;

public record PaymentCommand(
        String cardNumber,
        Integer expiryMonth,
        Integer expiryYear,
        String currency,
        Long amount,
        String cvv,
        String idempotencyKey) {

    @Override
    public String toString() {
        return "PaymentCommand[cardNumber=" + Sensitive.maskCardNumber(cardNumber)
                + ", expiryMonth=" + expiryMonth
                + ", expiryYear=" + expiryYear
                + ", currency=" + currency
                + ", amount=" + amount
                + ", cvv=***"
                + ", idempotencyKey=" + idempotencyKey + "]";
    }
}
