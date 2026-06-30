package com.paymentgateway.domain.service;

import com.paymentgateway.domain.model.Card;
import com.paymentgateway.domain.model.Money;
import com.paymentgateway.domain.model.Payment;
import com.paymentgateway.domain.port.in.PaymentRequest;
import com.paymentgateway.domain.port.out.BankResult;
import org.springframework.stereotype.Service;

@Service
public class ProcessPaymentUseCase {

    private final ProcessPaymentUseCaseSteps steps;

    public ProcessPaymentUseCase(ProcessPaymentUseCaseSteps steps) {
        this.steps = steps;
    }

    public Payment process(PaymentRequest request) {
        steps.handleValidationErrors(steps.validate(request));

        Card card = steps.toCard(request);
        Money money = steps.toMoney(request);
        Payment pending = steps.savePending(card, money);
        BankResult result = steps.authorize(card, money);
        return steps.settle(pending, result);
    }
}
