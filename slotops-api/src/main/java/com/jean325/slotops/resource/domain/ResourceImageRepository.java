package com.jean325.slotops.resource.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResourceImageRepository extends JpaRepository<ResourceImage, Long> {
    List<ResourceImage> findByResourceIdOrderBySortOrder(Long resourceId);
    void deleteByResourceId(Long resourceId);
}
