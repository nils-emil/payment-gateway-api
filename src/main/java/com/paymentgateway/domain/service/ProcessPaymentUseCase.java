package com.paymentgateway.domain.service;

import com.paymentgateway.domain.model.Payment;
import com.paymentgateway.domain.port.in.PaymentRequest;
import com.paymentgateway.domain.port.out.AuthorizationCommand;
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

        AuthorizationCommand command = steps.authorizationFor(request);
        Payment pending = steps.savePending(command);
        BankResult result = steps.authorize(command);
        return steps.settle(pending, result);
    }
}
