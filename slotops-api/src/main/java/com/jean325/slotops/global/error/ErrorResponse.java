package com.jean325.slotops.global.error;

import java.time.Instant;

public record ErrorResponse(
    int status,
    String error,
    String message,
    String path,
    Instant timestamp
) {}
