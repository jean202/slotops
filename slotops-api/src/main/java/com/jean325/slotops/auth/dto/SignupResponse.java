package com.jean325.slotops.auth.dto;

public record SignupResponse(
    Long userId,
    String email,
    String name
) {}
