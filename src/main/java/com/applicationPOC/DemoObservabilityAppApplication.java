package com.applicationPOC;

import java.lang.management.ManagementFactory;

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
		// DIAGNOSTIC CHECK: Ensure the JVM process has the necessary classes for Actuator's heap dump and diagnostics features.
		try {
            // Check for the class that Actuator requires
            Class.forName("com.sun.management.HotSpotDiagnosticMXBean");
            System.out.println(">>> [DIAGNOSTIC] SUCCESS: HotSpotDiagnosticMXBean is present in this IDE JVM process.");
            
            // Print the runtime name to ensure it's a JDK/HotSpot VM
            String vmName = ManagementFactory.getRuntimeMXBean().getVmName();
            System.out.println(">>> [DIAGNOSTIC] Running on VM: " + vmName);
        } catch (ClassNotFoundException e) {
            System.err.println(">>> [DIAGNOSTIC] CRITICAL: HotSpotDiagnosticMXBean is missing!");
            System.err.println(">>> [DIAGNOSTIC] Your IDE is launching the application using a JRE instead of a JDK.");
            System.err.println(">>> [DIAGNOSTIC] Current Java Home: " + System.getProperty("java.home"));
        }
		
		// Start the Spring Boot application
		SpringApplication.run(DemoObservabilityAppApplication.class, args);
	}
	
}
