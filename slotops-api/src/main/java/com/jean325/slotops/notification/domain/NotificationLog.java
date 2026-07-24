package com.jean325.slotops.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.OffsetDateTime;

/**
 * Record of a notification the system intended to send. For now it only logs
 * intent (delivery_status PENDING); an email/SMS provider integration will
 * later flip it to SENT/FAILED and stamp sent_at. Has no updated_at column,
 * so it does not extend BaseEntity.
 */
@Entity
@Table(name = "notification_logs")
public class NotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long reservationId;

    private Long customerId;

    @Enumerated(EnumType.STRING)
    private NotificationChannel channel;

    private String templateCode;

    @Enumerated(EnumType.STRING)
    private DeliveryStatus deliveryStatus;

    private String recipient;

    private OffsetDateTime sentAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected NotificationLog() {
    }

    public NotificationLog(
            Long reservationId,
            Long customerId,
            NotificationChannel channel,
            String templateCode,
            String recipient,
            Instant createdAt
    ) {
        this.reservationId = reservationId;
        this.customerId = customerId;
        this.channel = channel;
        this.templateCode = templateCode;
        this.deliveryStatus = DeliveryStatus.PENDING;
        this.recipient = recipient;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getTemplateCode() {
        return templateCode;
    }

    public DeliveryStatus getDeliveryStatus() {
        return deliveryStatus;
    }

    public NotificationChannel getChannel() {
        return channel;
    }

    public String getRecipient() {
        return recipient;
    }
}
