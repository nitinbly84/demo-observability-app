package com.applicationPOC.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
 
 @ExceptionHandler(Exception.class)
 public ResponseEntity<String> handleAll(Exception ex) {
     return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
             .body("Error occurred: " + ex.toString());
 }
}

