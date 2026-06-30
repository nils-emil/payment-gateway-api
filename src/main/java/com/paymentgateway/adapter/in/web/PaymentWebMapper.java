package com.paymentgateway.adapter.in.web;

import com.paymentgateway.domain.model.Payment;
import com.paymentgateway.domain.port.in.PaymentRequest;
import org.springframework.stereotype.Component;

@Component
public class PaymentWebMapper {

    public PaymentRequest toCommand(PaymentRequestDto dto) {
        return new PaymentRequest(
                dto.cardNumber(),
                dto.expiryMonth(),
                dto.expiryYear(),
                dto.currency(),
                dto.amount(),
                dto.cvv());
    }

    public PaymentResponseDto toResponse(Payment payment) {
        return new PaymentResponseDto(
                payment.id(),
                payment.status().displayName(),
                payment.lastFour(),
                payment.expiry().month(),
                payment.expiry().year(),
                payment.money().currency().code(),
                payment.money().amount(),
                payment.authorizationCode());
    }
}
