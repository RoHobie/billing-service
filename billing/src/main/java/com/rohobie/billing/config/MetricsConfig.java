package com.rohobie.billing.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Configuration;

/**
 * MetricsConfig
 *
 * Registers business-specific operational metrics with Micrometer for telemetry tracking.
 * Metrics include total billing runs executed, aggregate revenue generated, fraud anomalies flagged,
 * and computation execution duration.
 */
@Configuration
public class MetricsConfig {

    private final Counter billingRunsCounter;
    private final Counter revenuePaisaCounter;
    private final Counter fraudFlagsCounter;
    private final Timer billingRunTimer;

    public MetricsConfig(MeterRegistry registry) {
        this.billingRunsCounter = Counter.builder("fleet.billing.runs.total")
                .description("Total number of completed billing runs")
                .register(registry);

        this.revenuePaisaCounter = Counter.builder("fleet.billing.revenue.paisa")
                .description("Total cumulative revenue billed in paisa")
                .register(registry);

        this.fraudFlagsCounter = Counter.builder("fleet.billing.fraud.flags")
                .description("Total number of advisory fraud flags detected")
                .register(registry);

        this.billingRunTimer = Timer.builder("fleet.billing.run.duration")
                .description("Execution duration for month-end billing runs")
                .register(registry);
    }

    public void recordBillingRun(long totalPaisa, long durationNanos) {
        this.billingRunsCounter.increment();
        this.revenuePaisaCounter.increment(totalPaisa);
        this.billingRunTimer.record(java.time.Duration.ofNanos(durationNanos));
    }

    public void incrementFraudFlags(int count) {
        this.fraudFlagsCounter.increment(count);
    }

    public double getBillingRunCount() {
        return billingRunsCounter.count();
    }
}
