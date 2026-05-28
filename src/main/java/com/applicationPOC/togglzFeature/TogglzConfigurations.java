package com.applicationPOC.togglzFeature;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.togglz.core.Feature;
import org.togglz.core.manager.FeatureManager;
import org.togglz.core.manager.FeatureManagerBuilder;
import org.togglz.core.manager.TogglzConfig;
import org.togglz.core.repository.StateRepository;
import org.togglz.core.user.UserProvider;
import org.togglz.redis.RedisStateRepository;

import redis.clients.jedis.JedisPool;

@Configuration
public class TogglzConfigurations implements TogglzConfig {
	
//	private final ConditionalConfig conditionalConfig;
	private final UserProvider userProvider;

	@Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

	TogglzConfigurations(UserProvider userProvider) {
		this.userProvider = userProvider;
	}

	// Define the FeatureManager bean that will be used to manage feature toggles in the application
	@Bean
	FeatureManager featureManager() {
	    return new FeatureManagerBuilder()
	            .togglzConfig(this) // This forces Togglz to call your getUserProvider() method
	            .userProvider(this.userProvider) // Explicitly bind your security-aware provider bean
	            .build();
	}

	// Specify the class that contains the feature definitions. 
	// This class should implement the Feature interface and define the available features as enum constants.
	@Override
	public Class<? extends Feature> getFeatureClass() {
		return Features.class;
	}

	// Define the StateRepository that will be used to store the state of the feature toggles.
	// In this case, we are using RedisStateRepository but can also use  an in-memory repository, which is suitable for development and testing purposes.
	// For production, you might want to use a more persistent storage solution like a database or a distributed cache.
	//Check the documentation for more options: https://www.togglz.org/documentation/repositories
	@Override
	public StateRepository getStateRepository() {
		//		return new InMemoryStateRepository();
		// The Togglz Builder requires a JedisPool instance to know where Redis is.
		// Without this, it defaults to localhost.
		JedisPool jedisPool = new JedisPool(redisHost, redisPort);

		return new RedisStateRepository.Builder()
				.jedisPool(jedisPool)
				.keyPrefix("togglz-") // Matches your application-dev.properties
				.build();
	}

	// Define the UserProvider that will be used to determine the current user for feature toggle evaluation.
	@Override
	public UserProvider getUserProvider() {
		return this.userProvider;
	}

}
