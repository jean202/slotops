package com.jean325.slotops.availability.dto;

import java.time.LocalTime;

public record AvailabilityRuleResponse(
        Long id,
        Long resourceId,
        int dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        boolean isOpen,
        int leadTimeMinutes,
        LocalTime sameDayCutoffTime
) {}
