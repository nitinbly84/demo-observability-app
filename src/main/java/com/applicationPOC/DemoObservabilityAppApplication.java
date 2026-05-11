package com.applicationPOC;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.applicationPOC.config.UserProperties;

@io.swagger.v3.oas.annotations.OpenAPIDefinition(
	info = @io.swagger.v3.oas.annotations.info.Info(
		title = "Demo Observability App API",
		version = "1.0",
		description = "API documentation for Demo Observability App"
	)
)
@SpringBootApplication
@EnableCaching
@EnableAsync
@EnableMethodSecurity
@EnableTransactionManagement
@EnableAspectJAutoProxy
@EnableScheduling
@EnableConfigurationProperties(UserProperties.class)
public class DemoObservabilityAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoObservabilityAppApplication.class, args);
	}
	
}
