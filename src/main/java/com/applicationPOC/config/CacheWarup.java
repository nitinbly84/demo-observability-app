package com.applicationPOC.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class CacheWarup implements ApplicationRunner {

	@Override
	public void run(ApplicationArguments args) throws Exception {
		// Simulate cache warm-up logic here
		System.out.println("Warming up the cache...");
		// For example, you could load some data into the cache or perform any necessary initialization
		Thread.sleep(2000); // Simulating time-consuming cache warm-up
		System.out.println("Cache warm-up completed.");
	}

}
