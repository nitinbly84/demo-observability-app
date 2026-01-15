package com.applicationPOC.model;

import org.hibernate.validator.constraints.Length;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class UserDto {
    private Long id;
    @NotNull
    @Length(min = 7, max = 50, message = "{name.size.message}")
    @Pattern(regexp = "^[A-Za-z\\s]+$", message = "{name.invalid.message}")
    private String name;
    @Email(message = "Invalid email format")
    private String email;
    
 // getters and setters
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
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
