package com.jean325.slotops.location.dto;

import com.jean325.slotops.global.validation.ValidZoneId;
import jakarta.validation.constraints.NotBlank;

public record CreateLocationRequest(
    @NotBlank(message = "Location name cannot be blank")
    String name,

    @NotBlank(message = "Timezone cannot be blank")
    @ValidZoneId
    String timezone,

    String phone,

    String addressLine1,

    String addressLine2
) {}
