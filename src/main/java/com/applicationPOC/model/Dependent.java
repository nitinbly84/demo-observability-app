package com.applicationPOC.model;

import org.springframework.stereotype.Component;

@Component("dependent")
public class Dependent {
	
	public Dependent() {
		System.out.println("Dependent class constructor");
	}
}
