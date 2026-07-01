package com.paymentgateway.domain.usecase.processpayment;

import com.paymentgateway.domain.model.Payment;
import com.paymentgateway.domain.model.PaymentStatus;
import com.paymentgateway.domain.port.in.PaymentCommand;
import com.paymentgateway.domain.port.out.BankResult;
import com.paymentgateway.domain.port.out.PaymentMetrics;
import com.paymentgateway.domain.support.exception.BankUnavailableException;
import com.paymentgateway.domain.support.exception.PaymentInProgressException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ProcessPaymentUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProcessPaymentUseCase.class);

    private final ProcessPaymentUseCaseSteps steps;
    private final PaymentMetrics metrics;

    public ProcessPaymentUseCase(ProcessPaymentUseCaseSteps steps, PaymentMetrics metrics) {
        this.steps = steps;
        this.metrics = metrics;
    }

    public Payment process(PaymentCommand command) {
        return steps.findReplay(command)
                .map(this::replay)
                .orElseGet(() -> processNew(command));
    }

    private Payment replay(Payment existing) {
        if (existing.status() == PaymentStatus.PENDING) {
            throw new PaymentInProgressException(existing.id());
        }
        return existing;
    }

    private Payment processNew(PaymentCommand command) {
        steps.validate(command);
        Payment pending = steps.savePending(command);
        BankResult result;
        result = steps.authorize(command);
        Payment settled = steps.settle(pending, result);
        log.info("Payment {} {} amount={} {}", settled.id(), settled.status(),
                settled.money().amount(), settled.money().currency());
        metrics.recordOutcome(settled);
        return settled;
    }
}
