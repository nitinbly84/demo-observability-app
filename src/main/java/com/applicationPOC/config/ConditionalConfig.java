package com.applicationPOC.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class ConditionalConfig {

    @Bean
    @Profile("dev")
    String devOnlyBean() {
        return "dev-only-bean";
    }

    @Bean
    @ConditionalOnClass(name = "io.micrometer.core.instrument.MeterRegistry")
    String micrometerDependentBean() {
        return "micrometer-present";
    }
}
