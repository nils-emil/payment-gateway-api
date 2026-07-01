package com.paymentgateway.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI paymentGatewayOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Payment Gateway API")
                .description("Validates and forwards card payments to a simulated acquiring bank, "
                        + "and retrieves previously made payments.")
                .version("v1"));
    }
}
