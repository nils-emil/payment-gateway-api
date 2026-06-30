package com.paymentgateway.domain.service;

import com.paymentgateway.domain.model.*;
import com.paymentgateway.domain.port.in.PaymentRequest;
import com.paymentgateway.domain.port.in.ProcessPaymentUseCase;
import com.paymentgateway.domain.port.out.AcquiringBankClient;
import com.paymentgateway.domain.port.out.BankResult;
import com.paymentgateway.domain.port.out.PaymentRepository;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ProcessPaymentService implements ProcessPaymentUseCase {

    private final PaymentRepository repository;
    private final AcquiringBankClient bankClient;
    private final CurrencyAllowList allowList;
    private final Clock clock;

    public ProcessPaymentService(PaymentRepository repository, AcquiringBankClient bankClient,
                                 CurrencyAllowList allowList, Clock clock) {
        this.repository = repository;
        this.bankClient = bankClient;
        this.allowList = allowList;
        this.clock = clock;
    }

    @Override
    public Payment process(PaymentRequest request) {
        List<String> errors = new ArrayList<>();

        Currency currency = collect(errors, () -> Currency.of(request.currency()));
        ExpiryDate expiry = collect(errors, () -> ExpiryDate.of(request.expiryMonth(), request.expiryYear(), clock));
        Card card = collect(errors, () -> Card.of(request.cardNumber(), request.cvv(), expiry));
        Money money = currency == null ? null : collect(errors, () -> Money.of(currency, request.amount()));

        if (currency != null && !allowList.contains(currency)) {
            errors.add("currency " + request.currency() + " is not supported");
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        Payment payment = repository.save(Payment.pending(card.lastFour(), expiry, money));
        BankResult result = bankClient.authorize(card, money);
        Payment finalized = result.authorized()
                ? payment.authorize(result.authorizationCode())
                : payment.decline();
        return repository.save(finalized);
    }

    private <T> T collect(List<String> errors, Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (ValidationException e) {
            errors.addAll(e.errors());
            return null;
        }
    }
}
