package com.jean325.slotops.payment.domain;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Component;

/**
 * Tiered cancellation refund based on how far before the reservation start the
 * cancellation happens: 24h or more → full, 2h–24h → half, under 2h → none.
 */
@Component
public class RefundPolicy {

    private static final BigDecimal HALF = new BigDecimal("0.5");

    public BigDecimal refundRate(OffsetDateTime cancelledAt, OffsetDateTime startAt) {
        Duration untilStart = Duration.between(cancelledAt, startAt);
        if (untilStart.compareTo(Duration.ofHours(24)) >= 0) {
            return BigDecimal.ONE;
        }
        if (untilStart.compareTo(Duration.ofHours(2)) >= 0) {
            return HALF;
        }
        return BigDecimal.ZERO;
    }
}
