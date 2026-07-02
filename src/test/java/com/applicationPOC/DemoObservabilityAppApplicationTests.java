package com.applicationPOC;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

@SpringBootTest
class DemoObservabilityAppApplicationTests {

	//@Test
	void contextLoads() {
	}
	
	@Test
	void verifiesModularStructure() {
		ApplicationModules modules = ApplicationModules.of(DemoObservabilityAppApplication.class)
		.verify();
		new Documenter(modules).writeDocumentation();
	}

}
