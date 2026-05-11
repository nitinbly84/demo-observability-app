package com.applicationPOC.openAPI;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// This filter intercepts the Swagger UI HTML and injects custom CSS to hide the "Parameters" section when it's empty.
// This is a bit hacky, but it allows us to clean up the UI without needing to fork or customize the Swagger UI library itself.
// Note: This filter assumes that the Swagger UI is served at /swagger-ui/index.html, which is the default for springdoc-openapi.
// The CSS uses the :has() selector to conditionally hide the entire "Parameters" section if it only contains the "No parameters" message.
// For more complex UI customizations, you might consider hosting your own modified version of the Swagger UI, 
// but for this simple tweak, a filter is sufficient.
@Component
public class SwaggerUiCssFilter extends OncePerRequestFilter {

	private static final String CUSTOM_STYLE = """
			       <style>
			          /*
			           * Hide only the empty "No parameters" content block,
			           * NOT the whole section — so "Try it out" button is preserved.
			           */
			          .parameters-container:has(> .opblock-description-wrapper) {
			              display: none !important;
			          }
			       </style>
			    </head>
			    """;

	@Override
	protected void doFilterInternal(HttpServletRequest request,
			HttpServletResponse response,
			FilterChain chain)
					throws ServletException, IOException {

		String uri = request.getRequestURI();

		// Only intercept the Swagger UI entry point
		if (!uri.endsWith("/swagger-ui/index.html")) {
			chain.doFilter(request, response);
			return;
		}

		// Buffer the response so we can rewrite it
		ContentCachingResponseWrapper wrapper = new ContentCachingResponseWrapper(response);
		chain.doFilter(request, wrapper);

		// Inject our <style> block just before </head>
		String original = new String(wrapper.getContentAsByteArray(), StandardCharsets.UTF_8);
		String modified = original.replace("</head>", CUSTOM_STYLE);

		byte[] modifiedBytes = modified.getBytes(StandardCharsets.UTF_8);
		response.setContentLength(modifiedBytes.length);
		response.getOutputStream().write(modifiedBytes);
		// Do NOT call wrapper.copyBodyToResponse() — we wrote manually above
	}
}