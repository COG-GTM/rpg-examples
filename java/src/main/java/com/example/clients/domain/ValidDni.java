package com.example.clients.domain;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.ReportAsSingleViolation;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * CLIESRV {@code dniIsValid}: DNI must be {@code > 999999} and {@code < 100000000}.
 */
@Documented
@Min(ClientRules.DNI_MIN)
@Max(ClientRules.DNI_MAX)
@ReportAsSingleViolation
@Constraint(validatedBy = {})
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidDni {

    String message() default "DNI not valid";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
