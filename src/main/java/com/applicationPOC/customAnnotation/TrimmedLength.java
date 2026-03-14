package com.applicationPOC.customAnnotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Retention(RUNTIME)
@Target(FIELD)
@Constraint(validatedBy = TrimmedLengthValidator.class)
public @interface TrimmedLength {
    String message() default "Length is invalid after trimming";
    int min() default 0;
    int max() default 30;
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
