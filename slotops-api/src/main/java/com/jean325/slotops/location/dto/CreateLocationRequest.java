package com.jean325.slotops.location.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateLocationRequest(
    @NotBlank(message = "Location name cannot be blank")
    String name,

    @NotBlank(message = "Timezone cannot be blank")
    @Pattern(regexp = "^[A-Za-z_/]+$", message = "Invalid timezone format")
    String timezone,

    String phone,

    String addressLine1,

    String addressLine2
) {}
