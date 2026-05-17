package com.jean325.slotops.availability.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AvailabilityRuleRepository extends JpaRepository<AvailabilityRule, Long> {
    List<AvailabilityRule> findByResourceId(Long resourceId);
    Optional<AvailabilityRule> findByResourceIdAndDayOfWeek(Long resourceId, int dayOfWeek);
}
