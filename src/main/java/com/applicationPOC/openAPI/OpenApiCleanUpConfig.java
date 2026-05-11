package com.applicationPOC.openAPI;

import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// This configuration class serves two purposes:
// 1. It globally ignores certain types that are commonly added as "phantom" parameters
//    by SpringDoc but are not meaningful to API consumers (e.g. HttpServletRequest).
// 2. It defines a customizer that removes the "Parameters" key from the OpenAPI JSON
//    when the list of parameters is empty, which prevents the Swagger UI from showing
//    an empty "Parameters" section with a "No parameters" message.
@Configuration
public class OpenApiCleanUpConfig {
	
    static {
        // Globally ignore types that often leak as phantom parameters
        SpringDocUtils.getConfig()
            .addRequestWrapperToIgnore(
                jakarta.servlet.http.HttpServletResponse.class,
                jakarta.servlet.http.HttpServletRequest.class,
                org.springframework.validation.BindingResult.class,
                org.springframework.web.context.request.WebRequest.class
            );
    }

    // It is not required for now due to SwaggerUiCssFilter.java,
    // but it is a good backup to ensure the OpenAPI JSON is clean regardless of the UI.
    // This customizer iterates through all operations in the OpenAPI definition and checks if the parameters list is empty.
    // If it finds an operation with an empty parameters list, it sets that list to null.
    // This is a crucial step because in the OpenAPI JSON, 
    // an empty list of parameters would still result in a "Parameters" key being present with an empty array.
    // By setting it to null instead, we ensure that the "Parameters" key is omitted entirely from the JSON output for that operation.
    @Bean
    GlobalOpenApiCustomizer removeEmptyParametersCustomizer() {
        return openApi -> {
            if (openApi.getPaths() != null) {
                openApi.getPaths().values().forEach(pathItem -> 
                    pathItem.readOperations().forEach(operation -> {
                        // This is the key: if the list is empty, setting it to null 
                        // removes the 'Parameters' key from the JSON entirely.
                        if (operation.getParameters() != null && operation.getParameters().isEmpty()) {
                            operation.setParameters(null);
                        }
                    })
                );
            }
        };
    }
}