package com.jean325.slotops.availability.domain;

import com.jean325.slotops.resource.domain.Resource;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.OffsetDateTime;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "blackout_periods")
@EntityListeners(AuditingEntityListener.class)
public class BlackoutPeriod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resource_id")
    private Resource resource;

    private OffsetDateTime startAt;
    private OffsetDateTime endAt;
    private String reason;

    @CreatedDate
    private Instant createdAt;

    protected BlackoutPeriod() {}

    public BlackoutPeriod(Resource resource, OffsetDateTime startAt, OffsetDateTime endAt, String reason) {
        this.resource = resource;
        this.startAt = startAt;
        this.endAt = endAt;
        this.reason = reason;
    }

    public Long getId() { return id; }
    public Resource getResource() { return resource; }
    public OffsetDateTime getStartAt() { return startAt; }
    public OffsetDateTime getEndAt() { return endAt; }
    public String getReason() { return reason; }
    public Instant getCreatedAt() { return createdAt; }
}
