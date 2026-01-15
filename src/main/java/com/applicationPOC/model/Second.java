package com.applicationPOC.model;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
@Order(2) // To define the order of initialization if multiple classes have PostConstruct methods
public class Second {
	
	public Second() {
		System.out.println("Second class constructor.....");
	}
	
	@PostConstruct
	public void postConstruct() {
		System.out.println("Second class Post Construct method");
	}
}
