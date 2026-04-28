package com.jean325.slotops.location.domain;

import com.jean325.slotops.global.common.BaseEntity;
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

@Entity
@Table(name = "locations")
public class Location extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    private String name;

    private String timezone;

    @Enumerated(EnumType.STRING)
    private LocationStatus status;

    protected Location() {
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
