package com.jean325.slotops.global.error;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.Instant;

public record ErrorResponse(
    int status,
    String error,
    String message,
    String path,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    Instant timestamp
) {}
