package com.paymentgateway.adapter.in.web;

import com.paymentgateway.domain.model.BankUnavailableException;
import com.paymentgateway.domain.model.PaymentNotFoundException;
import com.paymentgateway.domain.model.ValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class PaymentExceptionHandler {

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidation(ValidationException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ValidationErrorResponse("Rejected", e.errors()));
    }

    @ExceptionHandler(BankUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleBankUnavailable(BankUnavailableException e) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ErrorResponse("acquiring bank unavailable"));
    }

    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(PaymentNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("payment not found"));
    }
}
