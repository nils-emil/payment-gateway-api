package com.paymentgateway.domain.port.in;

import com.paymentgateway.domain.model.Sensitive;

public record PaymentRequest(
        String cardNumber,
        int expiryMonth,
        int expiryYear,
        String currency,
        long amount,
        String cvv) {

    @Override
    public String toString() {
        return "PaymentRequest[cardNumber=" + Sensitive.maskCardNumber(cardNumber)
                + ", expiryMonth=" + expiryMonth
                + ", expiryYear=" + expiryYear
                + ", currency=" + currency
                + ", amount=" + amount
                + ", cvv=***]";
    }
}
