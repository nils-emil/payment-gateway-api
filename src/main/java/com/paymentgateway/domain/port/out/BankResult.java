package com.paymentgateway.domain.port.out;

public record BankResult(boolean authorized, String authorizationCode) {
}
