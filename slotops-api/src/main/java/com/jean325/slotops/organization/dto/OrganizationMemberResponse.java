package com.jean325.slotops.organization.dto;

import com.jean325.slotops.organization.domain.OrganizationMemberRole;
import java.time.Instant;

public record OrganizationMemberResponse(
    Long id,
    Long organizationId,
    Long userId,
    OrganizationMemberRole role,
    Instant joinedAt
) {}
