package com.paymentgateway.adapter.out.bank;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BankPaymentResponse(
        Boolean authorized,
        @JsonProperty("authorization_code") String authorizationCode) {
}
