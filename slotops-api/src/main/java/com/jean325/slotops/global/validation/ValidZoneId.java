package com.jean325.slotops.global.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** The value must be a time-zone id accepted by {@link java.time.ZoneId#of}. Null/blank is left to @NotBlank. */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidZoneIdValidator.class)
public @interface ValidZoneId {
    String message() default "Unknown timezone";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
