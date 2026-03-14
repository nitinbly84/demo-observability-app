package com.applicationPOC.model;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MultiAutowiredBean {

	private First first;
	private Second second;

	@Autowired
	public MultiAutowiredBean(First first) {
		this.first = first;
		System.out.println("MultiAutowiredBean class constructor");
	}

	// AAutowired on constructor is optional if there is only one constructor, 
	// but if there are multiple constructors then we need to specify which constructor to use for autowiring by using @Autowired annotation
	// and if we want to use both constructors then we can use @Autowired on both constructors and Spring will try to autowire the dependencies for both constructors
	// and if it finds the dependencies then it will use that constructor otherwise it will throw an error
	// If still you see this constructor being used then it may be used while creating the bean in ConditionalConfig class
	// and if you want to use the other constructor then you can remove the bean creation method from ConditionalConfig class
	// and let Spring autowire the dependencies using the constructor with @Autowired annotation
	//	@Autowired
	public MultiAutowiredBean(First first, Second second) {
		this.first = first;
		this.second = second;
		System.out.println("MultiAutowiredBean class constructor");
	}

	public String whoAmI() {
		if(second != null)
			return "I am MultiAutowiredBean and my dependencies say: " + first.whoAmI() + " & " + second.whoAmI();
		else
			return "I am MultiAutowiredBean and my dependency says: " + first.whoAmI();
	}

}
