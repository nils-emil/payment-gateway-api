package com.paymentgateway.adapter.out.metrics;

import com.paymentgateway.domain.model.Money;
import com.paymentgateway.domain.model.Payment;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MicrometerPaymentMetricsTest {

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final MicrometerPaymentMetrics metrics = new MicrometerPaymentMetrics(registry);

    private Payment authorized(long amount) {
        return Payment.pending(UUID.randomUUID(), null, "8877", 4, 2027, new Money(amount, "GBP"))
                .authorize("auth");
    }

    @Test
    void countsAndSumsByStatusAndCurrency() {
        metrics.recordOutcome(authorized(100));
        metrics.recordOutcome(authorized(250));

        var counter = registry.find("payments.processed")
                .tag("status", "AUTHORIZED").tag("currency", "GBP").counter();
        assertNotNull(counter);
        assertEquals(2.0, counter.count());

        var summary = registry.find("payments.amount")
                .tag("status", "AUTHORIZED").tag("currency", "GBP").summary();
        assertNotNull(summary);
        assertEquals(2, summary.count());
        assertEquals(350.0, summary.totalAmount());
    }

    @Test
    void separatesDeclinedFromAuthorized() {
        metrics.recordOutcome(Payment.pending(UUID.randomUUID(), null, "8877", 4, 2027, new Money(100, "GBP")).decline());

        assertNotNull(registry.find("payments.processed").tag("status", "DECLINED").counter());
        assertNull(registry.find("payments.processed").tag("status", "AUTHORIZED").counter());
    }
}
