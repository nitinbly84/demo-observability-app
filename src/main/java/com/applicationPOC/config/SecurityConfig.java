package com.applicationPOC.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.togglz.core.user.FeatureUser;
import org.togglz.core.user.SimpleFeatureUser;
import org.togglz.core.user.UserProvider;

import com.applicationPOC.security.JwtAuthFilter;

@Configuration
// Commented here to add it to main application class DemoObservabilityAppApplication.java
//@EnableMethodSecurity
public class SecurityConfig {

	private final JwtAuthFilter jwtAuthFilter;

	// Using @Lazy to avoid circular dependency issue with JwtAuthFilter and SecurityConfig
	@Lazy
	public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
		this.jwtAuthFilter = jwtAuthFilter;
	}

	@Bean
	SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
		.csrf(AbstractHttpConfigurer::disable)
		.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
		.authorizeHttpRequests(auth -> auth
				// Public endpoints
				.requestMatchers("/api/public/**").permitAll()
				.requestMatchers("/user/**").permitAll()
				.requestMatchers("/h2-console/**").permitAll()
				.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/auth/login").permitAll()
				.requestMatchers("/auth/login").permitAll()
				.requestMatchers("/vault/*").permitAll()
				.requestMatchers("/actuator/health", "/actuator/info").permitAll()
				// Actuator & secure APIs require auth
				.requestMatchers("/vault/manage/**").hasRole("ADMIN")
				.requestMatchers("/actuator/**").hasRole("ADMIN")
				.requestMatchers("/api/secure/**").authenticated()
				// --- TOGGLZ CONSOLE SECURITY RULES ---
				// RULE A: Strictly require ADMIN role for any modification or form submittals (POST, PUT, DELETE)
				.requestMatchers(org.springframework.http.HttpMethod.POST, "/togglz-console/**").hasRole("ADMIN")
				.requestMatchers(org.springframework.http.HttpMethod.PUT, "/togglz-console/**").hasRole("ADMIN")
				.requestMatchers(org.springframework.http.HttpMethod.DELETE, "/togglz-console/**").hasRole("ADMIN")

				// RULE B: Allow any authenticated user to send GET requests to see the index page and current states
				.requestMatchers(org.springframework.http.HttpMethod.GET, "/togglz-console/**").authenticated()
				.anyRequest().authenticated()
				)
		// allow H2 console frames
		.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
		.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
		//            .httpBasic(Customizer.withDefaults());

		return http.build();
	}

	/*
	 * @Bean UserDetailsService users() { UserDetails user =
	 * User.withUsername("user1") .password("{noop}password") .roles("USER")
	 * .build(); UserDetails admin = User.withUsername("admin")
	 * .password("{noop}password") .roles("ADMIN") .build(); return new
	 * InMemoryUserDetailsManager(user, admin); }
	 */

	/*
	 * @Bean UserDetailsService users(PasswordEncoder passwordEncoder) { UserDetails
	 * user = User.withUsername("user")
	 * .password(passwordEncoder.encode("password")) .roles("USER") .build();
	 * 
	 * UserDetails admin = User.withUsername("admin")
	 * .password(passwordEncoder.encode("password")) .roles("ADMIN") .build();
	 * 
	 * return new InMemoryUserDetailsManager(user, admin); }
	 */

	// Use DB-backed UserDetailsService
	/*
	 * @Bean UserDetailsService userDetailsService() { return
	 * databaseUserDetailsService; }
	 */

	@Bean
	AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
		return config.getAuthenticationManager();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		// BCrypt is the recommended default
		return new BCryptPasswordEncoder();
	}

	// CRITICAL FIX 2: Replace NoOpUserProvider with a Spring Security bridge
	@Bean
	UserProvider getUserProvider() {
		return new UserProvider() {
			@Override
			public FeatureUser getCurrentUser() {
				Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

				// If no token or not authenticated, deny Togglz admin state
				if (authentication == null || !authentication.isAuthenticated()) {
					return null;
				}

				String username = authentication.getName();

				// Check if the user has the authority "ROLE_ADMIN"
				// Since your HTTP chain uses .hasRole("ADMIN"), Spring prefixes it with "ROLE_" internally
				boolean isAdmin = authentication.getAuthorities().stream()
						.anyMatch(grantedAuthority -> grantedAuthority.getAuthority().contains("ADMIN"));

				// Return the FeatureUser stating explicitly whether they are allowed to toggle features
				return new SimpleFeatureUser(username, isAdmin);
			}
		};
	}

}
