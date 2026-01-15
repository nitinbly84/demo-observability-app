package com.applicationPOC.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CustomMetricsConfig {

    private final Counter demoRequestsCounter;

    public CustomMetricsConfig(MeterRegistry registry) {
        this.demoRequestsCounter = Counter.builder("demo.custom.requests")
                .description("Number of custom demo requests")
                .register(registry);
    }

    public void incrementDemoRequests() {
        demoRequestsCounter.increment();
    }
}

