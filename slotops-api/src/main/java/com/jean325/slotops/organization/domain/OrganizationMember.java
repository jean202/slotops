package com.jean325.slotops.organization.domain;

import com.jean325.slotops.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(
    name = "organization_members",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"organization_id", "user_id"})
    }
)
public class OrganizationMember extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(
        name = "organization_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_org_member_org")
    )
    private Organization organization;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrganizationMemberRole role;

    @Column(nullable = false)
    private Instant joinedAt;

    protected OrganizationMember() {}

    public OrganizationMember(Organization organization, Long userId, OrganizationMemberRole role) {
        this.organization = organization;
        this.userId = userId;
        this.role = role;
        this.joinedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Organization getOrganization() {
        return organization;
    }

    public Long getUserId() {
        return userId;
    }

    public OrganizationMemberRole getRole() {
        return role;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }

    public void setRole(OrganizationMemberRole role) {
        this.role = role;
    }
}
