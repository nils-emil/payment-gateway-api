package com.paymentgateway.domain.port.out;

public interface AcquiringBankClient {
    BankResult authorize(AuthorizationCommand command);
}
