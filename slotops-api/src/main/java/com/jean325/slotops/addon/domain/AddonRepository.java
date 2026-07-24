package com.jean325.slotops.addon.domain;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AddonRepository extends JpaRepository<Addon, Long> {

    List<Addon> findByOrganizationIdOrderByNameAsc(Long organizationId);

    List<Addon> findByIdInAndOrganizationIdAndStatus(
            Collection<Long> ids, Long organizationId, AddonStatus status);
}
