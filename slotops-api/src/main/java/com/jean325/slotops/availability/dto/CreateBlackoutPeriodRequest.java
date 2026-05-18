package com.jean325.slotops.availability.dto;

import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

public record CreateBlackoutPeriodRequest(
        @NotNull OffsetDateTime startAt,
        @NotNull OffsetDateTime endAt,
        String reason
) {}
