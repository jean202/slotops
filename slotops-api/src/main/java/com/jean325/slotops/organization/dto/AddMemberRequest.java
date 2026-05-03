package com.jean325.slotops.organization.dto;

import com.jean325.slotops.organization.domain.OrganizationMemberRole;
import jakarta.validation.constraints.NotNull;

public record AddMemberRequest(
    @NotNull(message = "User ID cannot be null")
    Long userId,

    @NotNull(message = "Role cannot be null")
    OrganizationMemberRole role
) {}
