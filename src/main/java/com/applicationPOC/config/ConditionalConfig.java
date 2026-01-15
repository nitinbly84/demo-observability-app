package com.applicationPOC.config;

import java.util.concurrent.Executor;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Configuration class demonstrating conditional bean creation based on profiles and classpath presence.
 * Also defines a custom ThreadPoolTaskExecutor bean named 'transcodingPoolTaskExecutor'.
 */
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

	@Bean(name = "transcodingPoolTaskExecutor")
	Executor transcodingPoolTaskExecutor() {
		final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(2);
		executor.setMaxPoolSize(2);
		executor.setQueueCapacity(500);
		executor.setThreadNamePrefix("Custom Pool-");
		// You should not call executor.initialize(), because it's called by Spring through InitializingBean.afterPropertiesSet()
		// executor.initialize();
		return executor;
	}
}
