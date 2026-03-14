package com.applicationPOC.config;

import java.util.concurrent.Executor;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.applicationPOC.model.First;
import com.applicationPOC.model.MultiAutowiredBean;
import com.applicationPOC.model.Second;

/**
 * Configuration class demonstrating conditional bean creation based on profiles and classpath presence.
 * Also defines a custom ThreadPoolTaskExecutor bean named 'transcodingPoolTaskExecutor'.
 */
@Configuration
public class ConditionalConfig {
	
	private First first;
	private Second second;
	
	@Bean("Multi")
	MultiAutowiredBean multiAutowiredBean(First first, Second second) {
		this.first = first;
		this.second = second;
		return new MultiAutowiredBean(first, second);
	}		

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
	
	// Conditional bean creation based on property value and shows that we can create 2 beans with same name but different conditions
	// otherwise duplicate bean error will occur due to same bean name
	
	@Bean("ConditionalFirst")
	@ConditionalOnProperty(name = "demo.first.enabled", havingValue = "true", matchIfMissing = true)
	First getConditionalFirst() {
		System.out.println("ConditionalFirst bean with havingValue true created");
		return new First("true");
	}
	
	@Bean("ConditionalFirst")
	@ConditionalOnProperty(name = "demo.first.enabled", havingValue = "false", matchIfMissing = true)
	First getConditionalFirst1() {
		System.out.println("ConditionalFirst bean with havingValue false created");
		return new First("false");
	}
	
}
