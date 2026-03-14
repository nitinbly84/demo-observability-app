package com.applicationPOC.customAnnotation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class TrimmedLengthValidator implements ConstraintValidator<TrimmedLength, String> {
    private int min;
    private int max;

    @Override
    public void initialize(TrimmedLength constraintAnnotation) {
        this.min = constraintAnnotation.min();
        this.max = constraintAnnotation.max();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) return true; // Let @NotNull handle nulls
        
        String trimmed = value.trim();
        int length = trimmed.length();
        
        return length >= min && length <= max;
    }
}
