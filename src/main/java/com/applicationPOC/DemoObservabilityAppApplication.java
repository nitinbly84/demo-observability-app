package com.applicationPOC;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableCaching
@EnableAsync
@EnableMethodSecurity
@EnableTransactionManagement
@EnableAspectJAutoProxy
public class DemoObservabilityAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoObservabilityAppApplication.class, args);
	}
	
}
