package com.paymentgateway.domain.service;

import com.paymentgateway.domain.model.Payment;
import com.paymentgateway.domain.port.in.PaymentRequest;
import com.paymentgateway.domain.port.out.BankResult;
import com.paymentgateway.domain.service.ProcessPaymentUseCaseSteps.ValidatedPayment;
import org.springframework.stereotype.Service;

@Service
public class ProcessPaymentUseCase {

    private final ProcessPaymentUseCaseSteps steps;

    public ProcessPaymentUseCase(ProcessPaymentUseCaseSteps steps) {
        this.steps = steps;
    }

    public Payment process(PaymentRequest request) {
        ValidatedPayment validated = steps.validate(request);
        Payment pending = steps.savePending(validated);
        BankResult result = steps.authorize(validated);
        return steps.settle(pending, result);
    }
}
