package com.paymentgateway.domain.model;

import org.junit.jupiter.api.Test;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

class CardTest {
    private final Clock clock = Clock.fixed(Instant.parse("2026-06-15T00:00:00Z"), ZoneOffset.UTC);
    private final ExpiryDate expiry = ExpiryDate.of(4, 2027, clock);

    @Test
    void acceptsValidCardAndExposesLastFour() {
        Card card = Card.of("2222405343248877", "123", expiry);
        assertEquals("8877", card.lastFour());
        assertEquals("2222405343248877", card.number());
        assertEquals("123", card.cvv());
    }

    @Test
    void acceptsBoundaryNumberLengths() {
        assertEquals("1234", Card.of("12345678901234", "123", expiry).lastFour());        // 14
        assertEquals("6789", Card.of("1234567890123456789", "123", expiry).lastFour());   // 19
    }

    @Test
    void acceptsFourDigitCvv() {
        assertEquals("1234", Card.of("2222405343248877", "1234", expiry).cvv());
    }

    @Test
    void rejectsShortOrLongNumber() {
        assertThrows(ValidationException.class, () -> Card.of("1234567890123", "123", expiry));   // 13
        assertThrows(ValidationException.class, () -> Card.of("12345678901234567890", "123", expiry)); // 20
    }

    @Test
    void toStringMasksSensitiveData() {
        Card card = Card.of("2222405343248877", "123", expiry);
        assertEquals("Card[lastFour=8877]", card.toString());
        assertFalse(card.toString().contains("2222405343248877"));
        assertFalse(card.toString().contains("123"));
    }

    @Test
    void rejectsNonNumericNumber() {
        assertThrows(ValidationException.class, () -> Card.of("222240534324887X", "123", expiry));
    }

    @Test
    void rejectsBadCvv() {
        assertThrows(ValidationException.class, () -> Card.of("2222405343248877", "12", expiry));   // 2
        assertThrows(ValidationException.class, () -> Card.of("2222405343248877", "12345", expiry)); // 5
        assertThrows(ValidationException.class, () -> Card.of("2222405343248877", "12a", expiry));
    }
}
