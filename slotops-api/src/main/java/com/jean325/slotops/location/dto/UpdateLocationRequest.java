package com.jean325.slotops.location.dto;

import com.jean325.slotops.location.domain.LocationStatus;

public record UpdateLocationRequest(
    String name,
    String phone,
    String addressLine1,
    String addressLine2,
    LocationStatus status
) {}
