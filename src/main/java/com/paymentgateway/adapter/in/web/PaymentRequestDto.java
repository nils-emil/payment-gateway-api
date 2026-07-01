package com.paymentgateway.adapter.in.web;

import com.paymentgateway.domain.support.util.Sensitive;

public record PaymentRequestDto(
        String cardNumber,
        Integer expiryMonth,
        Integer expiryYear,
        String currency,
        Long amount,
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
