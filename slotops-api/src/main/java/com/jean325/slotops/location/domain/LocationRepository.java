package com.jean325.slotops.location.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LocationRepository extends JpaRepository<Location, Long> {
    List<Location> findByOrganizationId(Long organizationId);
    Optional<Location> findByIdAndOrganizationId(Long id, Long organizationId);
    List<Location> findByOrganizationIdAndStatus(Long organizationId, LocationStatus status);
}
