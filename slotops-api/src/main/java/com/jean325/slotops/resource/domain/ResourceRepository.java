package com.jean325.slotops.resource.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ResourceRepository extends JpaRepository<Resource, Long> {
    Optional<Resource> findByIdAndStatus(Long id, ResourceStatus status);

    @Query("select r from Resource r where r.id = :id and r.location.id = :locationId")
    Optional<Resource> findByIdAndLocationId(@Param("id") Long id, @Param("locationId") Long locationId);

    @Query("select r from Resource r where r.id = :id and r.organization.id = :organizationId")
    Optional<Resource> findByIdAndOrganizationId(@Param("id") Long id, @Param("organizationId") Long organizationId);

    @Query("select r from Resource r where r.location.id = :locationId")
    List<Resource> findByLocationId(@Param("locationId") Long locationId);

    @Query("select r from Resource r where r.location.id = :locationId and r.status = :status")
    List<Resource> findByLocationIdAndStatus(@Param("locationId") Long locationId, @Param("status") ResourceStatus status);

    @Query("select r from Resource r where r.organization.id = :organizationId")
    List<Resource> findByOrganizationId(@Param("organizationId") Long organizationId);
}
