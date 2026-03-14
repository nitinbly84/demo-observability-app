package com.applicationPOC.customAnnotation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

@Component
public class DynamicMinValidator implements ConstraintValidator<DynamicMin, String> {

	@Autowired
	private Environment env;

	private int minLength;
	private int maxLength;

	@Override
	public void initialize(DynamicMin constraint) {
		String minValue = env.getProperty(constraint.min(), "8");
		this.minLength = Integer.parseInt(minValue);
		String maxValue = env.getProperty(constraint.max(), "18");
		this.maxLength = Integer.parseInt(maxValue);
	}

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		if (value == null)
			return true;
		return value.length() >= minLength && value.length() <= maxLength;
	}
}
