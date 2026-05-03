package com.jean325.slotops.resource.dto;

import com.jean325.slotops.resource.domain.ResourceStatus;
import com.jean325.slotops.resource.domain.ResourceType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ResourceResponse(
    Long id,
    Long organizationId,
    Long locationId,
    ResourceType resourceType,
    String name,
    String description,
    Integer capacity,
    Integer slotUnitMinutes,
    Integer minBookingMinutes,
    Integer maxBookingMinutes,
    Integer bufferMinutes,
    BigDecimal basePrice,
    String currency,
    ResourceStatus status,
    List<String> imageUrls,
    Instant createdAt,
    Instant updatedAt
) {}
