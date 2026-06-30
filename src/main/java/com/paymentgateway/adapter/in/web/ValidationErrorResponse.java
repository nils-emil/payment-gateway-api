package com.paymentgateway.adapter.in.web;

import com.paymentgateway.domain.model.ValidationError;

import java.util.List;

public record ValidationErrorResponse(String status, List<ValidationError> errors) {
}
