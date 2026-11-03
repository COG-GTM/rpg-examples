package com.example.clients.api;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** DNI must satisfy {@code dniIsValid}: 1,000,000..99,999,999. */
@Documented
@Constraint(validatedBy = DniValidator.class)
@Target({ElementType.PARAMETER, ElementType.FIELD, ElementType.METHOD, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidDni {

    String message() default "DNI not valid";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
