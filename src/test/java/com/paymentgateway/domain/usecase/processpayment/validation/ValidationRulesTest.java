package com.paymentgateway.domain.usecase.processpayment.validation;

import com.paymentgateway.domain.model.CurrencyAllowList;
import com.paymentgateway.domain.model.ValidationError;
import com.paymentgateway.domain.port.in.PaymentCommand;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ValidationRulesTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-06-15T00:00:00Z"), ZoneOffset.UTC);

    private PaymentCommand request(String card, Integer month, Integer year, String currency, Long amount, String cvv) {
        return new PaymentCommand(card, month, year, currency, amount, cvv, null);
    }

    @Test
    void cardNumberRule() {
        var rule = new CardNumberValidationRule();
        assertTrue(rule.validate(request("2222405343248877", 4, 2027, "GBP", 100L, "123")).isEmpty());
        assertEquals(1, rule.validate(request("123", 4, 2027, "GBP", 100L, "123")).size());
        assertEquals(1, rule.validate(request(null, 4, 2027, "GBP", 100L, "123")).size());
        assertEquals(1, rule.validate(request("1234567890123", 4, 2027, "GBP", 100L, "123")).size());
    }

    @Test
    void cvvRule() {
        var rule = new CvvValidationRule();
        assertTrue(rule.validate(request("2222405343248877", 4, 2027, "GBP", 100L, "1234")).isEmpty());
        assertEquals(1, rule.validate(request("2222405343248877", 4, 2027, "GBP", 100L, "1")).size());
        assertEquals(1, rule.validate(request("2222405343248877", 4, 2027, "GBP", 100L, null)).size());
    }

    @Test
    void amountRule() {
        var rule = new AmountValidationRule();
        assertTrue(rule.validate(request("2222405343248877", 4, 2027, "GBP", 100L, "123")).isEmpty());
        assertEquals(1, rule.validate(request("2222405343248877", 4, 2027, "GBP", 0L, "123")).size());
    }

    @Test
    void amountRuleDistinguishesMissingFromInvalid() {
        var rule = new AmountValidationRule();
        List<ValidationError> missing = rule.validate(request("2222405343248877", 4, 2027, "GBP", null, "123"));
        assertEquals("amount.required", missing.get(0).code());
        List<ValidationError> invalid = rule.validate(request("2222405343248877", 4, 2027, "GBP", -5L, "123"));
        assertEquals("amount.invalid", invalid.get(0).code());
    }

    @Test
    void expiryRule() {
        var rule = new ExpiryValidationRule(clock);
        assertTrue(rule.validate(request("2222405343248877", 4, 2027, "GBP", 100L, "123")).isEmpty());
        assertFalse(rule.validate(request("2222405343248877", 0, 2027, "GBP", 100L, "123")).isEmpty());
        assertFalse(rule.validate(request("2222405343248877", 13, 2027, "GBP", 100L, "123")).isEmpty());
        assertFalse(rule.validate(request("2222405343248877", 5, 2026, "GBP", 100L, "123")).isEmpty());
        assertFalse(rule.validate(request("2222405343248877", 4, 1_000_000_000, "GBP", 100L, "123")).isEmpty());
    }

    @Test
    void expiryRuleRejectsNon4DigitYearWithClearError() {
        var rule = new ExpiryValidationRule(clock);
        List<ValidationError> errors = rule.validate(request("2222405343248877", 4, 25, "GBP", 100L, "123"));
        assertEquals("expiry.year.invalid", errors.get(0).code());
    }

    @Test
    void expiryRuleDistinguishesMissingFromInvalid() {
        var rule = new ExpiryValidationRule(clock);
        List<ValidationError> missing = rule.validate(request("2222405343248877", null, null, "GBP", 100L, "123"));
        assertEquals("expiry.month.required", missing.get(0).code());
        assertEquals("expiry.year.required", missing.get(1).code());
    }

    @Test
    void currencyRule() {
        var rule = new CurrencyValidationRule(new CurrencyAllowList(Set.of("GBP", "USD", "EUR")));
        assertTrue(rule.validate(request("2222405343248877", 4, 2027, "GBP", 100L, "123")).isEmpty());
        assertTrue(rule.validate(request("2222405343248877", 4, 2027, "JPY", 100L, "123")).toString().contains("not supported"));
        assertFalse(rule.validate(request("2222405343248877", 4, 2027, "gb", 100L, "123")).isEmpty());
        assertFalse(rule.validate(request("2222405343248877", 4, 2027, null, 100L, "123")).isEmpty());
    }

    @Test
    void cardNumberRuleAcceptsLengthBoundaries() {
        var rule = new CardNumberValidationRule();
        assertTrue(rule.validate(request("30569309025904", 4, 2027, "GBP", 100L, "123")).isEmpty());
        assertTrue(rule.validate(request("2222405343248877006", 4, 2027, "GBP", 100L, "123")).isEmpty());
    }

    @Test
    void cardNumberRuleRejectsOutsideLengthBoundaries() {
        var rule = new CardNumberValidationRule();
        assertEquals(1, rule.validate(request("3056930902590", 4, 2027, "GBP", 100L, "123")).size());
        assertEquals(1, rule.validate(request("22224053432488770066", 4, 2027, "GBP", 100L, "123")).size());
    }

    @Test
    void cvvRuleBoundaries() {
        var rule = new CvvValidationRule();
        assertTrue(rule.validate(request("2222405343248877", 4, 2027, "GBP", 100L, "123")).isEmpty());
        assertEquals(1, rule.validate(request("2222405343248877", 4, 2027, "GBP", 100L, "12345")).size());
        assertEquals(1, rule.validate(request("2222405343248877", 4, 2027, "GBP", 100L, "12a")).size());
    }

    @Test
    void expiryRuleAcceptsCurrentMonth() {
        var rule = new ExpiryValidationRule(clock);
        assertTrue(rule.validate(request("2222405343248877", 6, 2026, "GBP", 100L, "123")).isEmpty());
    }
}
