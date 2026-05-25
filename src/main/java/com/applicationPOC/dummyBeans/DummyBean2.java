package com.applicationPOC.dummyBeans;

import org.springframework.stereotype.Component;

@Component
public class DummyBean2 implements ParentDummy {
	
	String name = "DummyBean2";
	
	@Override
	public String getBeanName() {
		return name;
	}

}
