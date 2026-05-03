package com.jean325.slotops.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record TokenRefreshRequest(
    @NotBlank(message = "Refresh token cannot be blank")
    String refreshToken
) {}
