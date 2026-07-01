package com.paymentgateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties(prefix = "payment")
public record PaymentProperties(Bank bank, Currencies currencies) {

    public record Bank(String baseUrl,
                       @DefaultValue("2s") Duration connectTimeout,
                       @DefaultValue("5s") Duration readTimeout) {}

    public record Currencies(List<String> allowed) {}
}
