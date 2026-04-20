package com.applicationPOC.togglzFeature;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.togglz.core.Feature;
import org.togglz.core.manager.FeatureManager;
import org.togglz.core.manager.FeatureManagerBuilder;
import org.togglz.core.manager.TogglzConfig;
import org.togglz.core.repository.StateRepository;
import org.togglz.core.repository.mem.InMemoryStateRepository;
import org.togglz.core.user.NoOpUserProvider;
import org.togglz.core.user.UserProvider;

@Configuration
public class TogglzConfigurations implements TogglzConfig {

	@Bean
	FeatureManager featureManager(UserProvider userProvider) {
		return new FeatureManagerBuilder().togglzConfig(this)
				.userProvider(userProvider)
				.build();
	}

	@Override
	public Class<? extends Feature> getFeatureClass() {
		return Features.class;
	}

	@Override
	public StateRepository getStateRepository() {
		return new InMemoryStateRepository();
	}

	@Bean
	@Override
	public UserProvider getUserProvider() {
		return new NoOpUserProvider();
	}

}
