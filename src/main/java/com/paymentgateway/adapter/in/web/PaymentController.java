package com.paymentgateway.adapter.in.web;

import com.paymentgateway.domain.model.Payment;
import com.paymentgateway.domain.port.in.GetPaymentUseCase;
import com.paymentgateway.domain.port.in.ProcessPaymentUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<PaymentResponseDto> process(@RequestBody PaymentRequestDto request) {
        Payment payment = processPaymentUseCase.process(mapper.toCommand(request));
        return ResponseEntity.ok(mapper.toResponse(payment));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponseDto> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(mapper.toResponse(getPaymentUseCase.getById(id)));
    }
}
