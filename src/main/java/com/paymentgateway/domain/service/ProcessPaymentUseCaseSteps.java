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

    public List<ValidationError> validate(PaymentRequest request) {
        List<ValidationError> errors = new ArrayList<>();
        for (ValidationRule<PaymentRequest> rule : validationRules) {
            errors.addAll(rule.validate(request));
        }
        return errors;
    }

    public void handleValidationErrors(List<ValidationError> errors) {
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }

    public Card toCard(PaymentRequest request) {
        ExpiryDate expiry = ExpiryDate.of(request.expiryMonth(), request.expiryYear(), clock);
        return Card.of(request.cardNumber(), request.cvv(), expiry);
    }

    public Money toMoney(PaymentRequest request) {
        return Money.of(Currency.of(request.currency()), request.amount());
    }

    public Payment savePending(Card card, Money money) {
        return repository.save(Payment.pending(card.lastFour(), card.expiry(), money));
    }

    public BankResult authorize(Card card, Money money) {
        return bankClient.authorize(card, money);
    }

    public Payment settle(Payment pending, BankResult result) {
        Payment finalized = result.authorized()
                ? pending.authorize(result.authorizationCode())
                : pending.decline();
        return repository.save(finalized);
    }
}
