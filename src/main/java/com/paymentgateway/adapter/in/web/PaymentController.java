package com.paymentgateway.adapter.in.web;

import com.paymentgateway.domain.model.Payment;
import com.paymentgateway.domain.usecase.getpayment.GetPaymentUseCase;
import com.paymentgateway.domain.usecase.processpayment.ProcessPaymentUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final ProcessPaymentUseCase processPaymentUseCase;
    private final GetPaymentUseCase getPaymentUseCase;
    private final PaymentWebMapper mapper;

    public PaymentController(ProcessPaymentUseCase processPaymentUseCase,
                             GetPaymentUseCase getPaymentUseCase,
                             PaymentWebMapper mapper) {
        this.processPaymentUseCase = processPaymentUseCase;
        this.getPaymentUseCase = getPaymentUseCase;
        this.mapper = mapper;
    }

    @PostMapping
    public ResponseEntity<PaymentResponseDto> process(
            @RequestBody PaymentRequestDto request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        Payment payment = processPaymentUseCase.process(mapper.toCommand(request, idempotencyKey));
        return ResponseEntity.ok(mapper.toResponse(payment));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponseDto> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(mapper.toResponse(getPaymentUseCase.getById(id)));
    }
}
