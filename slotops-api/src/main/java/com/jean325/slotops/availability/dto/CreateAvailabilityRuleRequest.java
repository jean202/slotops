package com.jean325.slotops.availability.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

public record CreateAvailabilityRuleRequest(
        @NotNull @Min(1) @Max(7) Integer dayOfWeek,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        @NotNull Boolean isOpen,
        @NotNull @Min(0) Integer leadTimeMinutes,
        LocalTime sameDayCutoffTime
) {}
