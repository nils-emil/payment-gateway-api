package com.paymentgateway.adapter.in.web;

import com.paymentgateway.domain.model.Sensitive;

public record PaymentRequestDto(
        String cardNumber,
        int expiryMonth,
        int expiryYear,
        String currency,
        long amount,
        String cvv) {

    @Override
    public String toString() {
        return "PaymentRequestDto[cardNumber=" + Sensitive.maskCardNumber(cardNumber)
                + ", expiryMonth=" + expiryMonth
                + ", expiryYear=" + expiryYear
                + ", currency=" + currency
                + ", amount=" + amount
                + ", cvv=***]";
    }
}
