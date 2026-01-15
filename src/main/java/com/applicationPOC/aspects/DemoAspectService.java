package com.applicationPOC.aspects;

import org.springframework.stereotype.Service;

@Service
public class DemoAspectService {
	
	public String serviceMethod(String name) {
		return "Hello, " + name + "! This is a response from DemoAspectService.";
	}

}
