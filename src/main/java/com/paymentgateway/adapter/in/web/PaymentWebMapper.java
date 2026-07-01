package com.paymentgateway.adapter.in.web;

import com.paymentgateway.domain.model.Payment;
import com.paymentgateway.domain.port.in.PaymentCommand;
import org.springframework.stereotype.Component;

@Component
public class PaymentWebMapper {

    public PaymentCommand toCommand(PaymentRequestDto dto, String idempotencyKey) {
        return new PaymentCommand(
                dto.cardNumber(),
                dto.expiryMonth(),
                dto.expiryYear(),
                dto.currency(),
                dto.amount(),
                dto.cvv(),
                idempotencyKey);
    }

    public PaymentResponseDto toResponse(Payment payment) {
        return new PaymentResponseDto(
                payment.id(),
                payment.status().displayName(),
                payment.lastFour(),
                payment.expiryMonth(),
                payment.expiryYear(),
                payment.money().currency(),
                payment.money().amount(),
                payment.authorizationCode());
    }
}
