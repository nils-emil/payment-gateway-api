package com.paymentgateway.domain.model;

import java.time.Clock;
import java.time.YearMonth;

public record ExpiryDate(int month, int year) {
    public ExpiryDate {
        if (month < 1 || month > 12) {
            throw new ValidationException("expiry month must be between 1 and 12");
        }
        if (year < 1 || year > 9999) {
            throw new ValidationException("expiry year must be between 1 and 9999");
        }
    }

    public static ExpiryDate of(int month, int year, Clock clock) {
        ExpiryDate expiry = new ExpiryDate(month, year);
        if (YearMonth.of(year, month).isBefore(YearMonth.now(clock))) {
            throw new ValidationException("card expiry must be in the future");
        }
        return expiry;
    }
}
