package com.paymentgateway.domain.usecase.processpayment;

import com.paymentgateway.domain.model.Money;
import com.paymentgateway.domain.model.Payment;
import com.paymentgateway.domain.model.ValidationError;
import com.paymentgateway.domain.port.in.PaymentCommand;
import com.paymentgateway.domain.port.out.AcquiringBankClient;
import com.paymentgateway.domain.port.out.AuthorizationCommand;
import com.paymentgateway.domain.port.out.BankResult;
import com.paymentgateway.domain.port.out.IdGenerator;
import com.paymentgateway.domain.port.out.PaymentRepository;
import com.paymentgateway.domain.support.exception.ValidationException;
import com.paymentgateway.domain.support.util.Sensitive;
import com.paymentgateway.domain.usecase.processpayment.validation.ValidationRule;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class ProcessPaymentUseCaseSteps {

    private final PaymentRepository repository;
    private final AcquiringBankClient bankClient;
    private final IdGenerator idGenerator;
    private final List<ValidationRule<PaymentCommand>> validationRules;

    public ProcessPaymentUseCaseSteps(PaymentRepository repository, AcquiringBankClient bankClient,
                                      IdGenerator idGenerator, List<ValidationRule<PaymentCommand>> validationRules) {
        this.repository = repository;
        this.bankClient = bankClient;
        this.idGenerator = idGenerator;
        this.validationRules = validationRules;
    }

    public Optional<Payment> findReplay(PaymentCommand command) {
        if (command.idempotencyKey() == null) {
            return Optional.empty();
        }
        return repository.findByIdempotencyKey(command.idempotencyKey());
    }

    public void validate(PaymentCommand command) {
        List<ValidationError> errors = new ArrayList<>();
        for (ValidationRule<PaymentCommand> rule : validationRules) {
            errors.addAll(rule.validate(command));
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }

    public Payment savePending(PaymentCommand command) {
        Money money = new Money(command.amount(), command.currency());
        Payment pending = Payment.pending(idGenerator.newId(), command.idempotencyKey(),
                Sensitive.lastFour(command.cardNumber()), command.expiryMonth(), command.expiryYear(), money);
        return repository.save(pending);
    }

    public BankResult authorize(PaymentCommand command) {
        AuthorizationCommand authorization = new AuthorizationCommand(
                command.cardNumber(),
                command.expiryMonth(),
                command.expiryYear(),
                new Money(command.amount(), command.currency()),
                command.cvv());
        return bankClient.authorize(authorization);
    }

    public Payment settle(Payment pending, BankResult result) {
        Payment finalized = result.authorized()
                ? pending.authorize(result.authorizationCode())
                : pending.decline();
        return repository.save(finalized);
    }
}
