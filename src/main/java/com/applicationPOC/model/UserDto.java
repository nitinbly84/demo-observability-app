package com.applicationPOC.model;

import org.hibernate.validator.constraints.Length;

import com.applicationPOC.customAnnotation.DynamicMin;
import com.applicationPOC.customAnnotation.TrimmedLength;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class UserDto {
    private Long id;
    @NotNull
    @Length(min = 7, max = 50, message = "{name.size.message}")
    @Pattern(regexp = "^[A-Za-z]([A-Za-z\\s]*[A-Za-z])?$", message = "{name.invalid.message}")
    private String name;
    @Email(message = "Invalid email format")
    private String email;
    
    @TrimmedLength(min = 7, max = 20, message = "{password.size.message}")
    private String password; // Add password field with validation
    
    @DynamicMin(min = "user.min.role.length", max = "user.max.role.length")
    private String role; // Add role field
    
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
		this.name = (name != null) ? name.stripLeading() : null;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String getRole() {
		return role;
	}
	public void setRole(String role) {
		this.role = role;
	}
}
