package com.jean325.slotops.payment.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

class RefundPolicyTest {

    private final RefundPolicy policy = new RefundPolicy();
    private final OffsetDateTime start = OffsetDateTime.parse("2026-06-15T10:00:00+09:00");

    @Test
    void fullRefundWhenAtLeast24HoursAhead() {
        assertThat(policy.refundRate(start.minusHours(24), start)).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(policy.refundRate(start.minusHours(72), start)).isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    void halfRefundBetweenTwoAndTwentyFourHours() {
        assertThat(policy.refundRate(start.minusHours(23), start)).isEqualByComparingTo(new BigDecimal("0.5"));
        assertThat(policy.refundRate(start.minusHours(2), start)).isEqualByComparingTo(new BigDecimal("0.5"));
    }

    @Test
    void noRefundUnderTwoHoursOrAfterStart() {
        assertThat(policy.refundRate(start.minusMinutes(119), start)).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(policy.refundRate(start, start)).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(policy.refundRate(start.plusHours(1), start)).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
