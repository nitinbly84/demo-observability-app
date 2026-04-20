package com.applicationPOC.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.togglz.core.manager.FeatureManager;
import org.togglz.core.util.NamedFeature;

import com.applicationPOC.togglzFeature.Features;

@Service
public class FeatureService {

	@Autowired
	private FeatureManager featureManager;

	public String isFeature1Available() {
		boolean active = featureManager
	    .isActive(new NamedFeature(Features.IS_FEATURE1_ENABLED.toString()));
		if(active) {
			return "Feature 1 is available";
		} else {
			return "Feature 1 is not available";
		}
	}
	
	public String isFeature2Available() {
		boolean active = featureManager
	    .isActive(new NamedFeature(Features.IS_FEATURE2_ENABLED.toString()));
		if(active) {
			return "Feature 2 is available";
		} else {
			return "Feature 2 is not available";
		}
	}
	
	public String isFeature3Available() {
		boolean active = featureManager
	    .isActive(new NamedFeature(Features.IS_FEATURE3_ENABLED.toString()));
		if(active) {
			return "Feature 3 is available";
		} else {
			return "Feature 3 is not available";
		}
	}

}
