package com.paymentgateway.domain.service;

import com.paymentgateway.domain.model.*;
import com.paymentgateway.domain.port.in.PaymentRequest;
import com.paymentgateway.domain.port.out.AcquiringBankClient;
import com.paymentgateway.domain.port.out.BankResult;
import com.paymentgateway.domain.port.out.PaymentRepository;
import com.paymentgateway.domain.service.validation.ValidationRule;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

@Component
public class ProcessPaymentUseCaseSteps {

    private final PaymentRepository repository;
    private final AcquiringBankClient bankClient;
    private final Clock clock;
    private final List<ValidationRule<PaymentRequest>> validationRules;

    public ProcessPaymentUseCaseSteps(PaymentRepository repository, AcquiringBankClient bankClient,
                                      Clock clock, List<ValidationRule<PaymentRequest>> validationRules) {
        this.repository = repository;
        this.bankClient = bankClient;
        this.clock = clock;
        this.validationRules = validationRules;
    }

    public ValidatedPayment validate(PaymentRequest request) {
        List<String> errors = new ArrayList<>();
        for (ValidationRule<PaymentRequest> rule : validationRules) {
            errors.addAll(rule.validate(request));
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        return build(request);
    }

    public Payment savePending(ValidatedPayment validated) {
        Card card = validated.card();
        return repository.save(Payment.pending(card.lastFour(), card.expiry(), validated.money()));
    }

    public BankResult authorize(ValidatedPayment validated) {
        return bankClient.authorize(validated.card(), validated.money());
    }

    public Payment settle(Payment pending, BankResult result) {
        Payment finalized = result.authorized()
                ? pending.authorize(result.authorizationCode())
                : pending.decline();
        return repository.save(finalized);
    }

    private ValidatedPayment build(PaymentRequest request) {
        Currency currency = Currency.of(request.currency());
        ExpiryDate expiry = ExpiryDate.of(request.expiryMonth(), request.expiryYear(), clock);
        Card card = Card.of(request.cardNumber(), request.cvv(), expiry);
        Money money = Money.of(currency, request.amount());
        return new ValidatedPayment(card, money);
    }

    public record ValidatedPayment(Card card, Money money) {
    }
}
