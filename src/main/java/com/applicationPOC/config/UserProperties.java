package com.applicationPOC.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Component
@Validated
@PropertySource("classpath:user.properties")
@ConfigurationProperties(prefix = "user.default") // This looks for "user.default.name" etc.
public class UserProperties {
	
	/**
     * The full name of the default system user.
     * Used for initial setup and audit logging.
     */
	@NotBlank(message = "Name must not be blank")
	private String name;
	
	/**
     * The contact email for the default user.
     * Must follow a valid email format.
     */
	@Email(message = "Email should be valid")
	private String email;

	// Getters and Setters
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

}
