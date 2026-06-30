package com.paymentgateway.adapter.out.bank;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BankPaymentRequest(
        @JsonProperty("card_number") String cardNumber,
        @JsonProperty("expiry_date") String expiryDate,
        String currency,
        long amount,
        String cvv) {
}
