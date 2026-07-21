package com.jean325.slotops.payment.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PaymentResponse(
        Long id,
        Long reservationId,
        String status,
        BigDecimal paidAmount,
        BigDecimal refundedAmount,
        String transactionRef,
        OffsetDateTime paidAt
) {
}
