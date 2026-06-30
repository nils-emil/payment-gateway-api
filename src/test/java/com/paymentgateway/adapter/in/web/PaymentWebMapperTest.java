package com.paymentgateway.adapter.in.web;

import com.paymentgateway.domain.model.*;
import com.paymentgateway.domain.port.in.PaymentRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PaymentWebMapperTest {

    private final PaymentWebMapper mapper = new PaymentWebMapper();

    @Test
    void mapsDtoToCommand() {
        PaymentRequestDto dto = new PaymentRequestDto("2222405343248877", 4, 2027, "GBP", 100, "123");
        PaymentRequest command = mapper.toCommand(dto);
        assertEquals("2222405343248877", command.cardNumber());
        assertEquals(4, command.expiryMonth());
        assertEquals(2027, command.expiryYear());
        assertEquals("GBP", command.currency());
        assertEquals(100, command.amount());
        assertEquals("123", command.cvv());
    }

    @Test
    void mapsAuthorizedPaymentToResponseWithDisplayStatus() {
        Payment payment = Payment.pending("8877", new ExpiryDate(4, 2027), Money.of(Currency.of("GBP"), 100))
                .authorize("auth-9");
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
        Payment payment = Payment.pending("8877", new ExpiryDate(4, 2027), Money.of(Currency.of("GBP"), 100));
        assertNull(mapper.toResponse(payment).authorizationCode());
        assertEquals("Pending", mapper.toResponse(payment).status());
    }

    @Test
    void declinedResponseHasNoAuthorizationCode() {
        Payment payment = Payment.pending("8877", new ExpiryDate(4, 2027), Money.of(Currency.of("GBP"), 100))
                .decline();
        PaymentResponseDto response = mapper.toResponse(payment);
        assertNull(response.authorizationCode());
        assertEquals("Declined", response.status());
    }
}
