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
	
	public String isFeature4Available() {
		boolean active = featureManager
	    .isActive(new NamedFeature(Features.IS_FEATURE4_ENABLED.toString()));
		if(active) {
			return "Feature 4 is available";
		} else {
			return "Feature 4 is not available";
		}
	}
	
	public String isFeature5Available() {
		boolean active = featureManager
	    .isActive(new NamedFeature(Features.IS_FEATURE5_ENABLED.toString()));
		if(active) {
			return "Feature 5 is available";
		} else {
			return "Feature 5 is not available";
		}
	}
	
	public String isFeature6Available() {
		boolean active = featureManager
	    .isActive(new NamedFeature(Features.IS_FEATURE6_ENABLED.toString()));
		if(active) {
			return "Feature 6 is available";
		} else {
			return "Feature 6 is not available";
		}
	}
	
	public String isFeature7Available() {
		boolean active = featureManager
	    .isActive(new NamedFeature(Features.IS_FEATURE7_ENABLED.toString()));
		if(active) {
			return "Feature 7 is available";
		} else {
			return "Feature 7 is not available";
		}
	}
	
	public String isFeature8Available() {
		boolean active = featureManager
	    .isActive(new NamedFeature(Features.IS_FEATURE8_ENABLED.toString()));
		if(active) {
			return "Feature 8 is available";
		} else {
			return "Feature 8 is not available";
		}
	}
	
	public String isFeature9Available() {
		boolean active = Features.IS_FEATURE9_ENABLED.isActive();
		if(active) {
			return "Feature 9 is available";
		} else {
			return "Feature 9 is not available";
		}
	}
	

}
