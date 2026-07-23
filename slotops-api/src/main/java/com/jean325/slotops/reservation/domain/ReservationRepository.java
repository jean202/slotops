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

    /**
     * Org-scoped reservation search. Every filter is optional: a null argument
     * leaves that condition out. {@code from} is inclusive, {@code to} exclusive.
     */
    @Query("""
            select r from Reservation r
            where r.organizationId = :orgId
              and (:status is null or r.status = :status)
              and (:from is null or r.startAt >= :from)
              and (:to is null or r.startAt < :to)
              and (:customerId is null or r.customer.id = :customerId)
            order by r.startAt desc
            """)
    List<Reservation> search(
            @Param("orgId") Long orgId,
            @Param("status") ReservationStatus status,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to,
            @Param("customerId") Long customerId
    );
}
