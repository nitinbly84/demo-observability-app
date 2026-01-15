package com.applicationPOC.model;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

// A bean with the given name is created for this class because of the @Component annotation during the server startup
// i.e. eager initialization
 @Component("scope1")
 @Scope("prototype") // Prototype scope bean and an instance is created during startup because of @Component annotation, so
 // on request instance count will start from 2, as one instance is already created during startup
public class Scope1 {
	
	private static int instanceCount;
	
	@Value("#{T(java.lang.Math).random() * 100}") // SpEL
	private int randomNumber;
	
	public Scope1() {
		System.out.println("Scope1 class constructor.....");
		instanceCount++;
	}
	
	public String getInstanceId() {
		return String.format("This is %d instance", instanceCount);
	}
	
	public int getNumber() {
		return randomNumber;
	}

}
