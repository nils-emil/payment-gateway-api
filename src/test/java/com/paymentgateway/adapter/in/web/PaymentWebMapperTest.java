package com.paymentgateway.adapter.in.web;

import com.paymentgateway.domain.model.*;
import com.paymentgateway.domain.port.in.PaymentCommand;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PaymentWebMapperTest {

    private final PaymentWebMapper mapper = new PaymentWebMapper();

    private Payment pending() {
        return Payment.pending(UUID.randomUUID(), null, "8877", 4, 2027, new Money(100, "GBP"));
    }

    @Test
    void mapsDtoToCommandWithIdempotencyKey() {
        PaymentRequestDto dto = new PaymentRequestDto("2222405343248877", 4, 2027, "GBP", 100L, "123");
        PaymentCommand command = mapper.toCommand(dto, "key-1");
        assertEquals("2222405343248877", command.cardNumber());
        assertEquals(4, command.expiryMonth());
        assertEquals(2027, command.expiryYear());
        assertEquals("GBP", command.currency());
        assertEquals(100, command.amount());
        assertEquals("123", command.cvv());
        assertEquals("key-1", command.idempotencyKey());
    }

    @Test
    void mapsAuthorizedPaymentToResponseWithDisplayStatus() {
        Payment payment = pending().authorize("auth-9");
        PaymentResponseDto response = mapper.toResponse(payment);
        assertEquals(payment.id(), response.id());
        assertEquals("Authorized", response.status());
        assertEquals("8877", response.lastFour());
        assertEquals(4, response.expiryMonth());
        assertEquals(2027, response.expiryYear());
        assertEquals("GBP", response.currency());
        assertEquals(100, response.amount());
        assertEquals("auth-9", response.authorizationCode());
    }

    @Test
    void pendingResponseHasNoAuthorizationCode() {
        Payment payment = pending();
        assertNull(mapper.toResponse(payment).authorizationCode());
        assertEquals("Pending", mapper.toResponse(payment).status());
    }

    @Test
    void declinedResponseHasNoAuthorizationCode() {
        Payment payment = pending().decline();
        PaymentResponseDto response = mapper.toResponse(payment);
        assertNull(response.authorizationCode());
        assertEquals("Declined", response.status());
    }
}
