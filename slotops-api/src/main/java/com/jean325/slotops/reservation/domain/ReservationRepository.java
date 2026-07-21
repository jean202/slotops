package com.jean325.slotops.reservation.domain;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    @Query("""
            select case when count(r) > 0 then true else false end
            from Reservation r
            where r.resource.id = :resourceId
              and r.status in :activeStatuses
              and r.startAt < :requestedEnd
              and r.endAt > :requestedStart
            """)
    boolean existsOverlap(
            @Param("resourceId") Long resourceId,
            @Param("requestedStart") OffsetDateTime requestedStart,
            @Param("requestedEnd") OffsetDateTime requestedEnd,
            @Param("activeStatuses") Collection<ReservationStatus> activeStatuses
    );

    @Query("""
            select r from Reservation r
            where r.resource.id = :resourceId
              and r.status in :activeStatuses
              and r.startAt < :windowEnd
              and r.endAt > :windowStart
            """)
    List<Reservation> findActiveForResourceBetween(
            @Param("resourceId") Long resourceId,
            @Param("windowStart") OffsetDateTime windowStart,
            @Param("windowEnd") OffsetDateTime windowEnd,
            @Param("activeStatuses") Collection<ReservationStatus> activeStatuses
    );

    List<Reservation> findByOrganizationIdOrderByStartAtDesc(Long organizationId);

    List<Reservation> findByOrganizationIdAndStatusOrderByStartAtDesc(
            Long organizationId, ReservationStatus status);
}
