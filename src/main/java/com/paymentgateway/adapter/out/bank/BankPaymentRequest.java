package com.paymentgateway.adapter.out.bank;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.paymentgateway.domain.support.util.Sensitive;

public record BankPaymentRequest(
        @JsonProperty("card_number") String cardNumber,
        @JsonProperty("expiry_date") String expiryDate,
        String currency,
        long amount,
        String cvv) {

    @Override
    public String toString() {
        return "BankPaymentRequest[cardNumber=" + Sensitive.maskCardNumber(cardNumber)
                + ", expiryDate=" + expiryDate
                + ", currency=" + currency
                + ", amount=" + amount
                + ", cvv=***]";
    }
}
