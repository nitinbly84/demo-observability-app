package com.applicationPOC.customAnnotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Retention(RUNTIME)
@Target(FIELD)
@Constraint(validatedBy = DynamicMinValidator.class)
public @interface DynamicMin {
    String message() default "{role.validLength.message}";
    String min() default "0";
    String max() default "30";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
