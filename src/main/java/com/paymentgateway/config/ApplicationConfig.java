package com.paymentgateway.config;

import com.paymentgateway.domain.model.CurrencyAllowList;
import com.paymentgateway.domain.port.in.GetPaymentUseCase;
import com.paymentgateway.domain.port.in.ProcessPaymentUseCase;
import com.paymentgateway.domain.port.out.AcquiringBankClient;
import com.paymentgateway.domain.port.out.PaymentRepository;
import com.paymentgateway.domain.service.GetPaymentService;
import com.paymentgateway.domain.service.ProcessPaymentService;
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

    @Bean
    public ProcessPaymentUseCase processPaymentUseCase(PaymentRepository repository,
                                                       AcquiringBankClient bankClient,
                                                       CurrencyAllowList allowList,
                                                       Clock clock) {
        return new ProcessPaymentService(repository, bankClient, allowList, clock);
    }

    @Bean
    public GetPaymentUseCase getPaymentUseCase(PaymentRepository repository) {
        return new GetPaymentService(repository);
    }
}
