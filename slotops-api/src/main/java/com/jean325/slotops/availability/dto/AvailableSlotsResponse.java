package com.jean325.slotops.availability.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record AvailableSlotsResponse(List<Slot> slots) {
    public record Slot(OffsetDateTime startAt, OffsetDateTime endAt) {}
}
