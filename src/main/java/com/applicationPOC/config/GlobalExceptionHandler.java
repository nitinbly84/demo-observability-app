package com.applicationPOC.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

// @ControllerAdvice to handle exceptions globally but we can limit its scope to specific packages
// Order of the methods below, doesn't matter, as specific exception will be checked for.
// If specific exception handler method not found, then generic one will be invoked.
@ControllerAdvice(basePackages = "com.applicationPOC.controller")
public class GlobalExceptionHandler {

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException ex) {
		return ResponseEntity.badRequest().body("Error: " + ex.getMessage());
	}

	@ExceptionHandler(AuthorizationDeniedException.class)
	public ResponseEntity<String> handleAll(Exception ex) {
		return ResponseEntity.status(HttpStatus.FORBIDDEN)
				.body("Error occurred: " + ex.toString());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<String> handleAllExceptions(Exception ex) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body("An unexpected error occurred: " + ex.toString());
	}
}

