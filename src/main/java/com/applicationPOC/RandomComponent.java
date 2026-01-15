package com.applicationPOC;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.applicationPOC.model.Scope1;

// It is suggested to use @Configuration but it is mainly used to denote configuration classes
// and we just need to register the class as some Spring component here.
@Component
public class RandomComponent {
	
	// Bean creation method in lazy initialization
	@Lazy
	@Bean(name = "scope1Bean") // Name of the bean to avoid confusion with the @Component bean of same class
	@ConditionalOnClass(com.applicationPOC.model.Dependent.class)
//	@Scope("singleton") // Default is singleton scope and can be omitted but added here for clarity and same can be added to Scope1.class file also
	@DependsOn("dependent") // To ensure 'dependent' bean is created before this bean
	Scope1 getScope() {
		System.out.println("Creating new Scope1 bean instance.......................");
		return new Scope1();
	}
}
