package com.applicationPOC.dummyBeans;

import org.springframework.stereotype.Component;

@Component
public class DummyBean1 implements ParentDummy {
	
	String name = "DummyBean1";
	
	@Override
	public String getBeanName() {
		return name;
	}

}
