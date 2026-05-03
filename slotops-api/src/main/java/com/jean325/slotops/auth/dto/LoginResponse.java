package com.jean325.slotops.auth.dto;

public record LoginResponse(
    Long userId,
    String email,
    String name,
    String accessToken,
    String refreshToken,
    long expiresIn
) {}
