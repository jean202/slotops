package com.jean325.slotops.reservation.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record AdminReservationResponse(
        Long id,
        String reservationCode,
        String status,
        ResourceSummary resource,
        CustomerSummary customer,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        Integer guestCount,
        BigDecimal totalAmount
) {
    public record ResourceSummary(Long id, String name) {
    }

    public record CustomerSummary(String name, String phone) {
    }
}
