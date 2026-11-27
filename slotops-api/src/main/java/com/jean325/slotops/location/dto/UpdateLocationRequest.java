package com.jean325.slotops.location.dto;

import com.jean325.slotops.location.domain.LocationStatus;
import jakarta.validation.constraints.Pattern;

// Partial update: null means "leave unchanged", but a provided value must satisfy the same rules as on create.
public record UpdateLocationRequest(
    @Pattern(regexp = "(?s).*\\S.*", message = "Location name cannot be blank")
    String name,
    String phone,
    String addressLine1,
    String addressLine2,
    LocationStatus status
) {}
