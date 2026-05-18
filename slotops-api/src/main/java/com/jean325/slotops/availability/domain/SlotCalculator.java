package com.jean325.slotops.availability.domain;

import com.jean325.slotops.reservation.domain.Reservation;
import com.jean325.slotops.resource.domain.Resource;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class SlotCalculator {

    private final ReservationConflictChecker conflictChecker;

    public SlotCalculator(ReservationConflictChecker conflictChecker) {
        this.conflictChecker = conflictChecker;
    }

    public record AvailableSlot(OffsetDateTime startAt, OffsetDateTime endAt) {}

    public List<AvailableSlot> calculate(
            Resource resource,
            List<AvailabilityRule> rules,
            List<BlackoutPeriod> blackouts,
            List<Reservation> reservations,
            LocalDate date,
            int durationMinutes,
            ZoneId timezone,
            OffsetDateTime now
    ) {
        int dayOfWeek = date.getDayOfWeek().getValue();
        AvailabilityRule rule = rules.stream()
                .filter(r -> r.getDayOfWeek() == dayOfWeek && r.isOpen())
                .findFirst()
                .orElse(null);
        if (rule == null) return List.of();

        // Same-day cutoff check
        if (date.equals(now.atZoneSameInstant(timezone).toLocalDate())
                && rule.getSameDayCutoffTime() != null
                && !now.atZoneSameInstant(timezone).toLocalTime().isBefore(rule.getSameDayCutoffTime())) {
            return List.of();
        }

        List<AvailableSlot> slots = new ArrayList<>();
        LocalTime cursor = rule.getStartTime();
        LocalTime latestStart = rule.getEndTime().minusMinutes(durationMinutes);

        while (!cursor.isAfter(latestStart)) {
            OffsetDateTime startAt = date.atTime(cursor).atZone(timezone).toOffsetDateTime();
            OffsetDateTime endAt = startAt.plusMinutes(durationMinutes);

            // Lead time check
            if (startAt.minusMinutes(rule.getLeadTimeMinutes()).isBefore(now)) {
                cursor = cursor.plusMinutes(resource.getSlotUnitMinutes());
                continue;
            }

            // Blackout check
            boolean blacked = blackouts.stream().anyMatch(b ->
                    b.getStartAt().isBefore(endAt) && b.getEndAt().isAfter(startAt));
            if (blacked) {
                cursor = cursor.plusMinutes(resource.getSlotUnitMinutes());
                continue;
            }

            // Conflict check
            if (!conflictChecker.hasConflict(reservations, startAt, endAt, resource.getBufferMinutes())) {
                slots.add(new AvailableSlot(startAt, endAt));
            }

            cursor = cursor.plusMinutes(resource.getSlotUnitMinutes());
        }

        return slots;
    }
}
