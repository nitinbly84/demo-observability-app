package com.applicationPOC.togglzFeature;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.applicationPOC.service.FeatureService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "FeatureController", description = "Feature APIs to check the status of individual features.") // Swagger/OpenAPI annotation to group APIs under a tag with description
@RestController
@RequestMapping("/featurerApi")
public class FeatureController {
	
	@Autowired
	private FeatureService featureService;

	@Operation(summary = "Check Feature endpoint", description = "Checks the availability of a feature based on the provided feature name, returns the availability status or an error message for invalid feature names"
			+ " Use http://localhost:8080/togglz-console to enable/disable features and test this endpoint")
	@GetMapping("/feature/{feature}")
	public String checkFeature(@PathVariable String feature) {
		return switch(feature) {
		case "feature1" -> featureService.isFeature1Available();
		case "feature2" -> featureService.isFeature2Available();
		case "feature3" -> featureService.isFeature3Available();
		case "feature4" -> featureService.isFeature4Available();
		case "feature5" -> featureService.isFeature5Available();
		case "feature6" -> featureService.isFeature6Available();
		case "feature7" -> featureService.isFeature7Available();
		case "feature8" -> featureService.isFeature8Available();
		case "feature9" -> featureService.isFeature9Available();
		default -> "Invalid feature name";
		};
	}
}
