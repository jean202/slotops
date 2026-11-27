package com.jean325.slotops.resource.dto;

import com.jean325.slotops.resource.domain.ResourceType;
import com.jean325.slotops.resource.dto.CreateResourceRequest.BookingRangeValid;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.math.BigDecimal;
import java.util.List;

@BookingRangeValid
public record CreateResourceRequest(
    @NotNull(message = "Resource type cannot be null")
    ResourceType resourceType,

    @NotBlank(message = "Resource name cannot be blank")
    String name,

    String description,

    @NotNull(message = "Capacity cannot be null")
    @Min(value = 1, message = "Capacity must be at least 1")
    Integer capacity,

    @NotNull(message = "Slot unit minutes cannot be null")
    @Min(value = 15, message = "Slot unit must be at least 15 minutes")
    Integer slotUnitMinutes,

    @NotNull(message = "Minimum booking minutes cannot be null")
    @Min(value = 15, message = "Minimum booking must be at least 15 minutes")
    Integer minBookingMinutes,

    @NotNull(message = "Maximum booking minutes cannot be null")
    @Min(value = 15, message = "Maximum booking must be at least 15 minutes")
    Integer maxBookingMinutes,

    @NotNull(message = "Buffer minutes cannot be null")
    @Min(value = 0, message = "Buffer minutes cannot be negative")
    Integer bufferMinutes,

    @NotNull(message = "Base price cannot be null")
    @DecimalMin(value = "0.0", inclusive = false, message = "Base price must be greater than 0")
    BigDecimal basePrice,

    @NotBlank(message = "Currency cannot be blank")
    String currency,

    List<String> imageUrls
) {

    /** minBookingMinutes must not exceed maxBookingMinutes; reported on maxBookingMinutes. */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = BookingRangeValidator.class)
    @interface BookingRangeValid {
        String message() default "Maximum booking minutes cannot be less than minimum booking minutes";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};
    }

    public static class BookingRangeValidator implements ConstraintValidator<BookingRangeValid, CreateResourceRequest> {
        @Override
        public boolean isValid(CreateResourceRequest request, ConstraintValidatorContext context) {
            Integer min = request.minBookingMinutes();
            Integer max = request.maxBookingMinutes();
            // Missing or out-of-range values are already reported by their own field constraints.
            if (min == null || max == null || min < 15 || max < 15 || min <= max) {
                return true;
            }
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode("maxBookingMinutes")
                .addConstraintViolation();
            return false;
        }
    }
}
