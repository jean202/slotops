package com.jean325.slotops.resource.domain;

import com.jean325.slotops.global.common.BaseEntity;
import com.jean325.slotops.location.domain.Location;
import com.jean325.slotops.organization.domain.Organization;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "resources")
public class Resource extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id")
    private Location location;

    @Enumerated(EnumType.STRING)
    private ResourceType resourceType;

    private String name;

    private String description;

    private Integer capacity;

    private Integer slotUnitMinutes;

    private Integer minBookingMinutes;

    private Integer maxBookingMinutes;

    private Integer bufferMinutes;

    private BigDecimal basePrice;

    private String currency;

    @Enumerated(EnumType.STRING)
    private ResourceStatus status;

    protected Resource() {
    }

    public Long getId() {
        return id;
    }

    public Long getOrganizationId() {
        return organization.getId();
    }

    public Long getLocationId() {
        return location.getId();
    }

    public String getName() {
        return name;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public Integer getSlotUnitMinutes() {
        return slotUnitMinutes;
    }

    public Integer getMinBookingMinutes() {
        return minBookingMinutes;
    }

    public Integer getMaxBookingMinutes() {
        return maxBookingMinutes;
    }

    public Integer getBufferMinutes() {
        return bufferMinutes;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public ResourceStatus getStatus() {
        return status;
    }

    public Location getLocation() {
        return location;
    }
}
