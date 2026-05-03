package com.jean325.slotops.organization.dto;

import com.jean325.slotops.organization.domain.OrganizationStatus;
import java.time.Instant;

public record OrganizationResponse(
    Long id,
    String name,
    String slug,
    Long ownerUserId,
    OrganizationStatus status,
    Instant createdAt,
    Instant updatedAt
) {}
