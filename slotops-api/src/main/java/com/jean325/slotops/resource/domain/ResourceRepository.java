package com.jean325.slotops.resource.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResourceRepository extends JpaRepository<Resource, Long> {
    Optional<Resource> findByIdAndStatus(Long id, ResourceStatus status);
    Optional<Resource> findByIdAndLocationId(Long id, Long locationId);
    Optional<Resource> findByIdAndOrganizationId(Long id, Long organizationId);
    List<Resource> findByLocationId(Long locationId);
    List<Resource> findByLocationIdAndStatus(Long locationId, ResourceStatus status);
    List<Resource> findByOrganizationId(Long organizationId);
}
