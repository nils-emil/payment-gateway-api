package com.paymentgateway.adapter.out.bank;

import com.paymentgateway.domain.support.exception.BankUnavailableException;
import com.paymentgateway.domain.port.out.AcquiringBankClient;
import com.paymentgateway.domain.port.out.AuthorizationCommand;
import com.paymentgateway.domain.port.out.BankResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class RestClientAcquiringBankClient implements AcquiringBankClient {

    private static final Logger log = LoggerFactory.getLogger(RestClientAcquiringBankClient.class);

    private final RestClient restClient;

    public RestClientAcquiringBankClient(RestClient bankRestClient) {
        this.restClient = bankRestClient;
    }

    @Override
    public BankResult authorize(AuthorizationCommand command) {
        BankPaymentRequest body = new BankPaymentRequest(
                command.cardNumber(),
                formatExpiry(command.expiryMonth(), command.expiryYear()),
                command.money().currency(),
                command.money().amount(),
                command.cvv());
        try {
            BankPaymentResponse response = restClient.post()
                    .uri("/payments")
                    .body(body)
                    .retrieve()
                    .body(BankPaymentResponse.class);
            if (!isReadable(response)) {
                throw new BankUnavailableException("acquiring bank returned an unreadable response", null);
            }
            return new BankResult(response.authorized(), response.authorizationCode());
        } catch (RestClientException e) {
            log.error("Acquiring bank call failed;", e);
            throw new BankUnavailableException("acquiring bank unavailable", e);
        }
    }

    private boolean isReadable(BankPaymentResponse response) {
        if (response == null || response.authorized() == null) {
            return false;
        }
        return !response.authorized() || (response.authorizationCode() != null && !response.authorizationCode().isBlank());
    }

    private String formatExpiry(int month, int year) {
        return String.format("%02d/%04d", month, year);
    }
}
