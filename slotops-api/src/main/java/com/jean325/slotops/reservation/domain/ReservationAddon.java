package com.jean325.slotops.reservation.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "reservation_addons")
public class ReservationAddon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long reservationId;

    private Long addonId;

    private Integer quantity;

    private BigDecimal unitPrice;

    private BigDecimal lineAmount;

    protected ReservationAddon() {
    }

    public ReservationAddon(Long reservationId, Long addonId, Integer quantity, BigDecimal unitPrice) {
        this.reservationId = reservationId;
        this.addonId = addonId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.lineAmount = unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public Long getId() {
        return id;
    }

    public Long getAddonId() {
        return addonId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public BigDecimal getLineAmount() {
        return lineAmount;
    }
}
