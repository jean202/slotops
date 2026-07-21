package com.jean325.slotops.payment.dto;

import com.jean325.slotops.payment.domain.PaymentStatus;
import jakarta.validation.constraints.NotNull;

public record UpdatePaymentRequest(
        @NotNull PaymentStatus status,
        String transactionRef
) {
}
