package com.applicationPOC.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

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
				.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
				.requestMatchers("/auth/login").permitAll()
				.requestMatchers("/actuator/health", "/actuator/info").permitAll()
				// Actuator & secure APIs require auth
				.requestMatchers("/actuator/**").hasRole("ADMIN")
				.requestMatchers("/api/secure/**").authenticated()
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

}
