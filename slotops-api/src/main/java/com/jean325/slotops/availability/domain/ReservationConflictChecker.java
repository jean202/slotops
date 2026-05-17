package com.jean325.slotops.availability.domain;

import com.jean325.slotops.reservation.domain.Reservation;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ReservationConflictChecker {

    public boolean hasConflict(
            List<Reservation> reservations,
            OffsetDateTime requestedStart,
            OffsetDateTime requestedEnd,
            int bufferMinutes
    ) {
        OffsetDateTime windowStart = requestedStart.minusMinutes(bufferMinutes);
        OffsetDateTime windowEnd = requestedEnd.plusMinutes(bufferMinutes);
        return reservations.stream().anyMatch(r ->
                r.getStartAt().isBefore(windowEnd) && r.getEndAt().isAfter(windowStart)
        );
    }
}
