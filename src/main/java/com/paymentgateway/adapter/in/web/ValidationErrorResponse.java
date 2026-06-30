package com.paymentgateway.adapter.in.web;

import java.util.List;

public record ValidationErrorResponse(String status, List<String> errors) {
}
