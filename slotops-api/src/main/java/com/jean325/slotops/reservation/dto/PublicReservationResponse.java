package com.jean325.slotops.reservation.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PublicReservationResponse(
        Long id,
        String reservationCode,
        String status,
        LocationSummary location,
        ResourceSummary resource,
        CustomerSummary customer,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        Integer guestCount,
        Pricing pricing,
        String paymentStatus
) {
    public record LocationSummary(Long id, String name) {
    }

    public record ResourceSummary(Long id, String name) {
    }

    public record CustomerSummary(String name, String phone) {
    }

    public record Pricing(
            BigDecimal baseAmount,
            BigDecimal addonAmount,
            BigDecimal discountAmount,
            BigDecimal totalAmount
    ) {
    }
}
