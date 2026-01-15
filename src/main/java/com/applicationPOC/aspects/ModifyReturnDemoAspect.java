package com.applicationPOC.aspects;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

// 
@Aspect
@Component
public class ModifyReturnDemoAspect {
	
	// Example of Around advice to modify return value
	@Around("execution(* com.applicationPOC.aspects.DemoAspectService.serviceMethod(..))")
	public Object modifyReturnValue(ProceedingJoinPoint joinPoint) throws Throwable {
		// Proceed with the original method call
		Object returnValue = joinPoint.proceed();
		
		// Modify the return value
		if (returnValue instanceof String) {
			returnValue = ((String) returnValue).concat(" [Modified by Aspect]");
		}
		return returnValue;
	}
	
	// Example of Before advice with argument capture
	@Before("execution(* com.applicationPOC.aspects.DemoAspectService.serviceMethod(..)) && args(name)")
	public void beforeServiceMethod(JoinPoint joinPoint, String name) {
		System.out.println("Before Advice executed........");
		String methodName = joinPoint.getSignature().getName();
		if(name.equalsIgnoreCase("Nitin"))
			System.out.printf("Before advice: %s is about to be called with argument %s \n", methodName, name);
	}

}
