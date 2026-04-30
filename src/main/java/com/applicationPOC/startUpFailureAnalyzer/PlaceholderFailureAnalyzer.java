package com.applicationPOC.startUpFailureAnalyzer;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.util.PlaceholderResolutionException;

// This class analyzes failures related to unresolved placeholders in Spring Boot applications.
// When Spring Boot encounters a placeholder (e.g., "${secret.key1}") that cannot be resolved during startup, it throws a PlaceholderResolutionException.
// This analyzer captures the exception during the startup process and provides a detailed failure analysis, including the specific placeholder
// that caused the issue and actionable advice for resolving it. It doesn't handle runtime exceptions that occur after the application has started,
// but it is crucial for diagnosing configuration issues that prevent the application from starting successfully.
public class PlaceholderFailureAnalyzer extends AbstractFailureAnalyzer<PlaceholderResolutionException> {

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, PlaceholderResolutionException cause) {
        // The exception contains the unresolved placeholder (e.g., "${secret.key1}")
        String placeholder = cause.getPlaceholder();
        
        return new FailureAnalysis(
            cause.getMessage()+"\n"+rootFailure.getMessage(),
            "Update your application.properties/yaml or environment variables to include " + placeholder,
            cause
        );
    }
}
