package com.jean325.slotops.reservation.domain;

import com.jean325.slotops.customer.domain.Customer;
import com.jean325.slotops.global.common.BaseEntity;
import com.jean325.slotops.resource.domain.Resource;
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
import java.time.OffsetDateTime;

@Entity
@Table(name = "reservations")
public class Reservation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long organizationId;

    private Long locationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resource_id")
    private Resource resource;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    private String reservationCode;

    @Enumerated(EnumType.STRING)
    private ReservationStatus status;

    private OffsetDateTime startAt;

    private OffsetDateTime endAt;

    private Integer guestCount;

    private BigDecimal baseAmount;

    private BigDecimal addonAmount;

    private BigDecimal discountAmount;

    private BigDecimal totalAmount;

    private String specialRequest;

    protected Reservation() {
    }

    public Reservation(
            Long organizationId,
            Long locationId,
            Resource resource,
            Customer customer,
            String reservationCode,
            ReservationStatus status,
            OffsetDateTime startAt,
            OffsetDateTime endAt,
            Integer guestCount,
            BigDecimal baseAmount,
            BigDecimal addonAmount,
            BigDecimal discountAmount,
            BigDecimal totalAmount,
            String specialRequest
    ) {
        this.organizationId = organizationId;
        this.locationId = locationId;
        this.resource = resource;
        this.customer = customer;
        this.reservationCode = reservationCode;
        this.status = status;
        this.startAt = startAt;
        this.endAt = endAt;
        this.guestCount = guestCount;
        this.baseAmount = baseAmount;
        this.addonAmount = addonAmount;
        this.discountAmount = discountAmount;
        this.totalAmount = totalAmount;
        this.specialRequest = specialRequest;
    }

    public Long getId() {
        return id;
    }

    public String getReservationCode() {
        return reservationCode;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public Resource getResource() {
        return resource;
    }

    public Customer getCustomer() {
        return customer;
    }

    public OffsetDateTime getStartAt() {
        return startAt;
    }

    public OffsetDateTime getEndAt() {
        return endAt;
    }

    public Integer getGuestCount() {
        return guestCount;
    }

    public BigDecimal getBaseAmount() {
        return baseAmount;
    }

    public BigDecimal getAddonAmount() {
        return addonAmount;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }
}
