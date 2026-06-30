package com.paymentgateway.adapter.out.bank;

import com.paymentgateway.domain.model.BankUnavailableException;
import com.paymentgateway.domain.model.Card;
import com.paymentgateway.domain.model.Money;
import com.paymentgateway.domain.port.out.AcquiringBankClient;
import com.paymentgateway.domain.port.out.BankResult;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class RestClientAcquiringBankClient implements AcquiringBankClient {

    private final RestClient restClient;

    public RestClientAcquiringBankClient(RestClient bankRestClient) {
        this.restClient = bankRestClient;
    }

    @Override
    public BankResult authorize(Card card, Money money) {
        BankPaymentRequest body = new BankPaymentRequest(
                card.number(),
                formatExpiry(card),
                money.currency().code(),
                money.amount(),
                card.cvv());
        try {
            BankPaymentResponse response = restClient.post()
                    .uri("/payments")
                    .body(body)
                    .retrieve()
                    .body(BankPaymentResponse.class);
            if (response == null) {
                throw new BankUnavailableException("acquiring bank returned an empty response", null);
            }
            return new BankResult(response.authorized(), response.authorizationCode());
        } catch (RestClientException e) {
            throw new BankUnavailableException("acquiring bank unavailable", e);
        }
    }

    private String formatExpiry(Card card) {
        return String.format("%02d/%04d", card.expiry().month(), card.expiry().year());
    }
}
