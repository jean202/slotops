package com.jean325.slotops.payment.domain;

import com.jean325.slotops.global.common.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;

@Entity
@Table(name = "payments")
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long reservationId;

    private String paymentMethod;

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

    private String transactionRef;

    private BigDecimal paidAmount;

    private BigDecimal refundedAmount;

    private OffsetDateTime paidAt;

    protected Payment() {
    }

    public Payment(Long reservationId) {
        this.reservationId = reservationId;
        this.paymentStatus = PaymentStatus.UNPAID;
        this.paidAmount = BigDecimal.ZERO;
        this.refundedAmount = BigDecimal.ZERO;
    }

    /**
     * Applies a status change, keeping the money fields consistent:
     * PAID records the full amount and paid-at time; REFUNDED moves the paid
     * amount to refunded; other statuses only change the label.
     */
    public void applyStatus(PaymentStatus target, BigDecimal totalAmount, OffsetDateTime now, String transactionRef) {
        switch (target) {
            case PAID -> {
                this.paidAmount = totalAmount;
                this.paidAt = now;
            }
            case REFUNDED -> this.refundedAmount = this.paidAmount;
            case UNPAID, PARTIALLY_PAID -> {
                // label-only change
            }
        }
        if (transactionRef != null) {
            this.transactionRef = transactionRef;
        }
        this.paymentStatus = target;
    }

    /**
     * Refunds the given fraction of the amount already paid. A zero refund
     * (nothing paid, or a rate of 0) leaves the payment untouched.
     */
    public void applyRefund(BigDecimal rate) {
        BigDecimal amount = paidAmount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
        if (amount.signum() > 0) {
            this.refundedAmount = amount;
            this.paymentStatus = PaymentStatus.REFUNDED;
        }
    }

    public Long getId() {
        return id;
    }

    public Long getReservationId() {
        return reservationId;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public String getTransactionRef() {
        return transactionRef;
    }

    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    public BigDecimal getRefundedAmount() {
        return refundedAmount;
    }

    public OffsetDateTime getPaidAt() {
        return paidAt;
    }
}
