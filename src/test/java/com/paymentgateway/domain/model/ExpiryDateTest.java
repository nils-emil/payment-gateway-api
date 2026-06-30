package com.paymentgateway.domain.model;

import org.junit.jupiter.api.Test;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

class ExpiryDateTest {
    // Fixed "now" = 2026-06-15
    private final Clock clock = Clock.fixed(Instant.parse("2026-06-15T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void acceptsFutureMonthYear() {
        ExpiryDate expiry = ExpiryDate.of(4, 2027, clock);
        assertEquals(4, expiry.month());
        assertEquals(2027, expiry.year());
    }

    @Test
    void acceptsCurrentMonth() {
        assertDoesNotThrow(() -> ExpiryDate.of(6, 2026, clock));
    }

    @Test
    void rejectsPastMonth() {
        assertThrows(ValidationException.class, () -> ExpiryDate.of(5, 2026, clock));
    }

    @Test
    void rejectsMonthOutOfRange() {
        assertThrows(ValidationException.class, () -> ExpiryDate.of(0, 2027, clock));
        assertThrows(ValidationException.class, () -> ExpiryDate.of(13, 2027, clock));
    }

    @Test
    void rejectsAbsurdYearAsValidationErrorNotCrash() {
        assertThrows(ValidationException.class, () -> ExpiryDate.of(4, 1_000_000_000, clock));
    }
}
