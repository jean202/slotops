package com.jean325.slotops.staffnote.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Operator-authored note attached to a reservation. Has only created_at (no
 * updated_at), so it does not extend BaseEntity.
 */
@Entity
@Table(name = "staff_notes")
public class StaffNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long reservationId;

    private Long authorUserId;

    private String content;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected StaffNote() {
    }

    public StaffNote(Long reservationId, Long authorUserId, String content, Instant createdAt) {
        this.reservationId = reservationId;
        this.authorUserId = authorUserId;
        this.content = content;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Long getReservationId() {
        return reservationId;
    }

    public Long getAuthorUserId() {
        return authorUserId;
    }

    public String getContent() {
        return content;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
