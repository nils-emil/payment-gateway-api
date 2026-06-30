package com.paymentgateway.config;

import com.paymentgateway.domain.model.CurrencyAllowList;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.util.Set;

@Configuration
public class ApplicationConfig {

    @Bean
    public RestClient bankRestClient(PaymentProperties props) {
        return RestClient.builder().baseUrl(props.bank().baseUrl()).build();
    }

    @Bean
    public CurrencyAllowList currencyAllowList(PaymentProperties props) {
        return new CurrencyAllowList(Set.copyOf(props.currencies().allowed()));
    }

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
