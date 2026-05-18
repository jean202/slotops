package com.jean325.slotops.availability.dto;

import java.time.OffsetDateTime;

public record BlackoutPeriodResponse(
        Long id,
        Long resourceId,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        String reason
) {}
