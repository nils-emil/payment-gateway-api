package com.paymentgateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "payment")
public record PaymentProperties(Bank bank, Currencies currencies) {

    public record Bank(String baseUrl) {}
    public record Currencies(List<String> allowed) {}
}
