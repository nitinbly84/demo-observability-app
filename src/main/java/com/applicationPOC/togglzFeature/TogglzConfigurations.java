package com.applicationPOC.togglzFeature;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.togglz.core.Feature;
import org.togglz.core.manager.FeatureManager;
import org.togglz.core.manager.FeatureManagerBuilder;
import org.togglz.core.manager.TogglzConfig;
import org.togglz.core.repository.StateRepository;
import org.togglz.core.user.UserProvider;
import org.togglz.redis.RedisStateRepository;
import org.togglz.spring.security.SpringSecurityUserProvider;

@Configuration
public class TogglzConfigurations implements TogglzConfig {

	// Define the FeatureManager bean that will be used to manage feature toggles in the application
	@Bean
	FeatureManager featureManager(UserProvider userProvider) {
		return new FeatureManagerBuilder().togglzConfig(this)
				.build();
	}

	// Specify the class that contains the feature definitions. 
	// This class should implement the Feature interface and define the available features as enum constants.
	@Override
	public Class<? extends Feature> getFeatureClass() {
		return Features.class;
	}

	// Define the StateRepository that will be used to store the state of the feature toggles.
	// In this case, we are using an in-memory repository, which is suitable for development and testing purposes.
	// For production, you might want to use a more persistent storage solution like a database or a distributed cache.
	//Check the documentation for more options: https://www.togglz.org/documentation/repositories
	@Override
	public StateRepository getStateRepository() {
//		return new InMemoryStateRepository();
		return new RedisStateRepository.Builder()
				.build();
	}

	// Define the UserProvider that will be used to determine the current user for feature toggle evaluation.
	@Override
	public UserProvider getUserProvider() {
		return new SpringSecurityUserProvider("ADMIN_AUTHORITY");
	}

}
