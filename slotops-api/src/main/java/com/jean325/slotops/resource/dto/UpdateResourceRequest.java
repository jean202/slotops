package com.jean325.slotops.resource.dto;

import com.jean325.slotops.resource.domain.ResourceStatus;
import java.math.BigDecimal;

public record UpdateResourceRequest(
    String name,
    String description,
    Integer capacity,
    BigDecimal basePrice,
    ResourceStatus status
) {}
