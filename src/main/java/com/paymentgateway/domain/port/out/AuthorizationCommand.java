package com.paymentgateway.domain.port.out;

import com.paymentgateway.domain.model.Card;
import com.paymentgateway.domain.model.Currency;
import com.paymentgateway.domain.model.ExpiryDate;
import com.paymentgateway.domain.model.Money;
import com.paymentgateway.domain.port.in.PaymentRequest;

import java.time.Clock;
import java.util.UUID;

public record AuthorizationCommand(Card card, Money money, String idempotencyKey) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private PaymentRequest request;
        private Clock clock;
        private String idempotencyKey;

        public Builder request(PaymentRequest request) {
            this.request = request;
            return this;
        }

        public Builder clock(Clock clock) {
            this.clock = clock;
            return this;
        }

        public Builder idempotencyKey(String idempotencyKey) {
            this.idempotencyKey = idempotencyKey;
            return this;
        }

        public AuthorizationCommand build() {
            ExpiryDate expiry = ExpiryDate.of(request.expiryMonth(), request.expiryYear(), clock);
            Card card = Card.of(request.cardNumber(), request.cvv(), expiry);
            Money money = Money.of(Currency.of(request.currency()), request.amount());
            String key = idempotencyKey != null ? idempotencyKey : UUID.randomUUID().toString();
            return new AuthorizationCommand(card, money, key);
        }
    }
}
