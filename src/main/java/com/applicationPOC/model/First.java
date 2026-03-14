package com.applicationPOC.model;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Component
@Order(1) // To set the order of initialization if multiple classes have @PostConstruct methods
public class First {
	String msg;
	
	public First() {
		System.out.println("First class constructor......");
	}
	
	public First(String msg) {
		this.msg = msg;
	}
	
	@PostConstruct
	public void postConstruct() {
		System.out.println("First class Post Construct method");
	}
	
	@PreDestroy
	public void predestroy() {
		System.out.println("First class Pre Destroy method");
	}
	
	@EventListener(ApplicationReadyEvent.class)
	public void afterAllInit() {
		System.out.println("First class Application Ready Event method");
	}
	
	public String whoAmI() {
		if (msg != null) {
			return "I am First class bean with demo.first.enabled property value as " + msg;
		}
		return "I am First class bean";
	}
	
}
