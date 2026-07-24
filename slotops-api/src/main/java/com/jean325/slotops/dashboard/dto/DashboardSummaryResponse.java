package com.jean325.slotops.dashboard.dto;

import java.math.BigDecimal;
import java.util.Map;

public record DashboardSummaryResponse(
        long totalReservations,
        Map<String, Long> reservationsByStatus,
        BigDecimal confirmedRevenue
) {
}
