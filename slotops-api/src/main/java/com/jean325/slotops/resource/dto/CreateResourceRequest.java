package com.jean325.slotops.resource.dto;

import com.jean325.slotops.resource.domain.ResourceType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

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
) {}
