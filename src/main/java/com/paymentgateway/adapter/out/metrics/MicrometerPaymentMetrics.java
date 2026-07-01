package com.paymentgateway.adapter.out.metrics;

import com.paymentgateway.domain.model.Payment;
import com.paymentgateway.domain.port.out.PaymentMetrics;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class MicrometerPaymentMetrics implements PaymentMetrics {

    private final MeterRegistry registry;

    public MicrometerPaymentMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void recordOutcome(Payment payment) {
        String status = payment.status().name();
        String currency = payment.money().currency();
        Counter.builder("payments.processed")
                .description("Number of payments processed, by terminal outcome")
                .tag("status", status)
                .tag("currency", currency)
                .register(registry)
                .increment();
        DistributionSummary.builder("payments.amount")
                .description("Processed payment amounts in the minor currency unit")
                .baseUnit("minor")
                .tag("status", status)
                .tag("currency", currency)
                .register(registry)
                .record(payment.money().amount());
    }
}
