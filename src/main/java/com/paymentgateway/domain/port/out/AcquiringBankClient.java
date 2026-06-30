package com.paymentgateway.domain.port.out;

import com.paymentgateway.domain.model.Card;
import com.paymentgateway.domain.model.Money;

public interface AcquiringBankClient {
    BankResult authorize(Card card, Money money);
}
