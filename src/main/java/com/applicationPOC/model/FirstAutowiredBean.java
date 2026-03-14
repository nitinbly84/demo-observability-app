package com.applicationPOC.model;

import org.springframework.stereotype.Component;

@Component
public class FirstAutowiredBean {
	
	private First first;
	
	public FirstAutowiredBean(First first) {
		this.first = first;
		System.out.println("FirstAutowiredBean class constructor");
	}
	
	public String whoAmI() {
		return "I am FirstAutowiredBean and my dependency says: " + first.whoAmI();
	}

}
