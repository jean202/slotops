package com.jean325.slotops.availability.domain;

import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BlackoutPeriodRepository extends JpaRepository<BlackoutPeriod, Long> {

    @Query("""
            select b from BlackoutPeriod b
            where b.resource.id = :resourceId
              and b.startAt < :windowEnd
              and b.endAt > :windowStart
            """)
    List<BlackoutPeriod> findOverlappingBetween(
            @Param("resourceId") Long resourceId,
            @Param("windowStart") OffsetDateTime windowStart,
            @Param("windowEnd") OffsetDateTime windowEnd
    );

    @Query("""
            select case when count(b) > 0 then true else false end
            from BlackoutPeriod b
            where b.resource.id = :resourceId
              and b.startAt < :endAt
              and b.endAt > :startAt
            """)
    boolean existsOverlap(
            @Param("resourceId") Long resourceId,
            @Param("startAt") OffsetDateTime startAt,
            @Param("endAt") OffsetDateTime endAt
    );

    List<BlackoutPeriod> findByResourceId(Long resourceId);
}
