package com.jean325.slotops.resource.dto;

import com.jean325.slotops.resource.domain.ResourceStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

// Partial update: null means "leave unchanged", but a provided value must satisfy the same rules as on create.
public record UpdateResourceRequest(
    @Pattern(regexp = "(?s).*\\S.*", message = "Resource name cannot be blank")
    String name,

    String description,

    @Min(value = 1, message = "Capacity must be at least 1")
    Integer capacity,

    @DecimalMin(value = "0.0", inclusive = false, message = "Base price must be greater than 0")
    BigDecimal basePrice,

    ResourceStatus status
) {}
