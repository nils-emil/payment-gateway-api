package com.paymentgateway.domain.port.out;

import com.paymentgateway.domain.model.Money;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuthorizationCommandTest {

    @Test
    void toStringMasksCardNumberAndCvv() {
        AuthorizationCommand command = new AuthorizationCommand("2222405343248877", 4, 2027, new Money(100, "GBP"), "123");
        String text = command.toString();
        assertTrue(text.contains("****8877"), text);
        assertFalse(text.contains("2222405343248877"), text);
        assertFalse(text.contains("123"), text);
    }
}
