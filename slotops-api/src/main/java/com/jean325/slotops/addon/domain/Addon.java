package com.jean325.slotops.addon.domain;

import com.jean325.slotops.global.common.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "addons")
public class Addon extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long organizationId;

    private String name;

    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    private AddonStatus status;

    protected Addon() {
    }

    public Addon(Long organizationId, String name, BigDecimal price) {
        this.organizationId = organizationId;
        this.name = name;
        this.price = price;
        this.status = AddonStatus.ACTIVE;
    }

    public Long getId() {
        return id;
    }

    public Long getOrganizationId() {
        return organizationId;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public AddonStatus getStatus() {
        return status;
    }
}
