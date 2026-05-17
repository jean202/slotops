package com.jean325.slotops.availability.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AvailabilityRuleRepository extends JpaRepository<AvailabilityRule, Long> {

    @Query("select r from AvailabilityRule r where r.resource.id = :resourceId")
    List<AvailabilityRule> findByResourceId(@Param("resourceId") Long resourceId);

    @Query("select r from AvailabilityRule r where r.resource.id = :resourceId and r.dayOfWeek = :dayOfWeek")
    Optional<AvailabilityRule> findByResourceIdAndDayOfWeek(@Param("resourceId") Long resourceId, @Param("dayOfWeek") int dayOfWeek);
}
