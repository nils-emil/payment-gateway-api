package com.paymentgateway.domain.service.validation;

import com.paymentgateway.domain.model.CurrencyAllowList;
import com.paymentgateway.domain.port.in.PaymentRequest;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ValidationRulesTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-06-15T00:00:00Z"), ZoneOffset.UTC);

    private PaymentRequest request(String card, int month, int year, String currency, long amount, String cvv) {
        return new PaymentRequest(card, month, year, currency, amount, cvv);
    }

    @Test
    void cardNumberRule() {
        var rule = new CardNumberValidationRule();
        assertTrue(rule.validate(request("2222405343248877", 4, 2027, "GBP", 100, "123")).isEmpty());
        assertEquals(1, rule.validate(request("123", 4, 2027, "GBP", 100, "123")).size());
        assertEquals(1, rule.validate(request(null, 4, 2027, "GBP", 100, "123")).size());
    }

    @Test
    void cvvRule() {
        var rule = new CvvValidationRule();
        assertTrue(rule.validate(request("2222405343248877", 4, 2027, "GBP", 100, "1234")).isEmpty());
        assertEquals(1, rule.validate(request("2222405343248877", 4, 2027, "GBP", 100, "1")).size());
    }

    @Test
    void amountRule() {
        var rule = new AmountValidationRule();
        assertTrue(rule.validate(request("2222405343248877", 4, 2027, "GBP", 100, "123")).isEmpty());
        assertEquals(1, rule.validate(request("2222405343248877", 4, 2027, "GBP", 0, "123")).size());
    }

    @Test
    void expiryRule() {
        var rule = new ExpiryValidationRule(clock);
        assertTrue(rule.validate(request("2222405343248877", 4, 2027, "GBP", 100, "123")).isEmpty());
        assertFalse(rule.validate(request("2222405343248877", 13, 2027, "GBP", 100, "123")).isEmpty());
        assertFalse(rule.validate(request("2222405343248877", 5, 2026, "GBP", 100, "123")).isEmpty());
        assertFalse(rule.validate(request("2222405343248877", 4, 1_000_000_000, "GBP", 100, "123")).isEmpty());
    }

    @Test
    void currencyRule() {
        var rule = new CurrencyValidationRule(new CurrencyAllowList(Set.of("GBP", "USD", "EUR")));
        assertTrue(rule.validate(request("2222405343248877", 4, 2027, "GBP", 100, "123")).isEmpty());
        assertTrue(rule.validate(request("2222405343248877", 4, 2027, "JPY", 100, "123")).toString().contains("not supported"));
        assertFalse(rule.validate(request("2222405343248877", 4, 2027, "gb", 100, "123")).isEmpty());
    }
}
