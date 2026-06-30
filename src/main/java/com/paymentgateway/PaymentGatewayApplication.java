package com.paymentgateway;

import com.paymentgateway.config.PaymentProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(PaymentProperties.class)
public class PaymentGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(PaymentGatewayApplication.class, args);
    }
}
