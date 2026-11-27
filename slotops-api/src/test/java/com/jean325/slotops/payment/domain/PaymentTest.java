package com.jean325.slotops.payment.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

class PaymentTest {

    private static final BigDecimal TOTAL = new BigDecimal("33333.00");
    private final OffsetDateTime now = OffsetDateTime.parse("2026-06-01T10:00:00+09:00");

    private Payment paid() {
        Payment payment = new Payment(1L);
        payment.applyStatus(PaymentStatus.PAID, TOTAL, now, "TX-1");
        return payment;
    }

    @Test
    void startsUnpaidWithZeroAmounts() {
        Payment payment = new Payment(7L);

        assertThat(payment.getReservationId()).isEqualTo(7L);
        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.UNPAID);
        assertThat(payment.getPaidAmount()).isEqualByComparingTo("0");
        assertThat(payment.getRefundedAmount()).isEqualByComparingTo("0");
        assertThat(payment.getPaidAt()).isNull();
    }

    @Test
    void payingRecordsFullAmountTimeAndReference() {
        Payment payment = paid();

        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(payment.getPaidAmount()).isEqualByComparingTo(TOTAL);
        assertThat(payment.getPaidAt()).isEqualTo(now);
        assertThat(payment.getTransactionRef()).isEqualTo("TX-1");
    }

    @Test
    void statusChangeWithoutReferenceKeepsExistingReference() {
        Payment payment = paid();

        payment.applyStatus(PaymentStatus.REFUNDED, TOTAL, now.plusDays(1), null);

        assertThat(payment.getTransactionRef()).isEqualTo("TX-1");
    }

    @Test
    void manualRefundMovesWholePaidAmountToRefunded() {
        Payment payment = paid();

        payment.applyStatus(PaymentStatus.REFUNDED, TOTAL, now.plusDays(1), null);

        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(payment.getRefundedAmount()).isEqualByComparingTo(TOTAL);
    }

    @Test
    void fullCancellationRefundReturnsEverythingPaid() {
        Payment payment = paid();

        payment.applyRefund(BigDecimal.ONE);

        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(payment.getRefundedAmount()).isEqualByComparingTo(TOTAL);
    }

    @Test
    void partialCancellationRefundIsRoundedHalfUpToTwoDecimals() {
        Payment payment = new Payment(1L);
        payment.applyStatus(PaymentStatus.PAID, new BigDecimal("100.01"), now, null);

        payment.applyRefund(new BigDecimal("0.5"));

        // 50.005 rounds up
        assertThat(payment.getRefundedAmount()).isEqualByComparingTo("50.01");
        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.REFUNDED);
    }

    @Test
    void zeroRateRefundLeavesPaidPaymentUntouched() {
        Payment payment = paid();

        payment.applyRefund(BigDecimal.ZERO);

        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(payment.getRefundedAmount()).isEqualByComparingTo("0");
    }

    @Test
    void refundOnUnpaidPaymentChangesNothing() {
        Payment payment = new Payment(1L);

        payment.applyRefund(BigDecimal.ONE);

        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.UNPAID);
        assertThat(payment.getRefundedAmount()).isEqualByComparingTo("0");
    }
}
