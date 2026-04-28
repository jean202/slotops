package com.jean325.slotops.resource.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResourceRepository extends JpaRepository<Resource, Long> {
    Optional<Resource> findByIdAndStatus(Long id, ResourceStatus status);
}
