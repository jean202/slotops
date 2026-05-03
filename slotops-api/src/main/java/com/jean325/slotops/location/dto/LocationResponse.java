package com.jean325.slotops.location.dto;

import com.jean325.slotops.location.domain.LocationStatus;
import java.time.Instant;

public record LocationResponse(
    Long id,
    Long organizationId,
    String name,
    String timezone,
    String phone,
    String addressLine1,
    String addressLine2,
    LocationStatus status,
    Instant createdAt,
    Instant updatedAt
) {}
