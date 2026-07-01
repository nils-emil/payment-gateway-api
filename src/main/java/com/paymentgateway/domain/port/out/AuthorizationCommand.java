package com.paymentgateway.domain.port.out;

import com.paymentgateway.domain.model.Money;
import com.paymentgateway.domain.support.util.Sensitive;

public record AuthorizationCommand(
        String cardNumber,
        int expiryMonth,
        int expiryYear,
        Money money,
        String cvv) {

    @Override
    public String toString() {
        return "AuthorizationCommand[cardNumber=" + Sensitive.maskCardNumber(cardNumber)
                + ", expiryMonth=" + expiryMonth
                + ", expiryYear=" + expiryYear
                + ", money=" + money
                + ", cvv=***]";
    }
}
