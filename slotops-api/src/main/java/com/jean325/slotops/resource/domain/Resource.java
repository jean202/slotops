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

    protected Resource() {}

    public Resource(
        Organization organization,
        Location location,
        ResourceType resourceType,
        String name,
        Integer capacity,
        Integer slotUnitMinutes,
        Integer minBookingMinutes,
        Integer maxBookingMinutes,
        Integer bufferMinutes,
        BigDecimal basePrice,
        String currency
    ) {
        this.organization = organization;
        this.location = location;
        this.resourceType = resourceType;
        this.name = name;
        this.capacity = capacity;
        this.slotUnitMinutes = slotUnitMinutes;
        this.minBookingMinutes = minBookingMinutes;
        this.maxBookingMinutes = maxBookingMinutes;
        this.bufferMinutes = bufferMinutes;
        this.basePrice = basePrice;
        this.currency = currency;
        this.status = ResourceStatus.ACTIVE;
    }

    public Long getId() {
        return id;
    }

    public Organization getOrganization() {
        return organization;
    }

    public Long getOrganizationId() {
        return organization.getId();
    }

    public Location getLocation() {
        return location;
    }

    public Long getLocationId() {
        return location.getId();
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
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

    public String getCurrency() {
        return currency;
    }

    public ResourceStatus getStatus() {
        return status;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }

    public void setStatus(ResourceStatus status) {
        this.status = status;
    }
}
