package com.jean325.slotops.organization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateOrganizationRequest(
    @NotBlank(message = "Organization name cannot be blank")
    String name,

    @NotBlank(message = "Slug cannot be blank")
    @Pattern(
        regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$",
        message = "Slug must be lowercase alphanumeric with hyphens only"
    )
    String slug
) {}
