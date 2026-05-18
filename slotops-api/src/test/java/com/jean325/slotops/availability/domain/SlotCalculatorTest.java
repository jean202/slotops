package com.jean325.slotops.availability.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.jean325.slotops.reservation.domain.Reservation;
import com.jean325.slotops.resource.domain.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SlotCalculatorTest {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final LocalDate MONDAY = LocalDate.of(2026, 5, 18); // ISO dayOfWeek = 1

    private SlotCalculator calculator;
    private Resource resource;
    private AvailabilityRule openRule;
    private OffsetDateTime pastNow;

    @BeforeEach
    void setUp() {
        calculator = new SlotCalculator(new ReservationConflictChecker());
        resource = stubResource(60, 60, 180, 0);
        openRule = new AvailabilityRule(null, 1, LocalTime.of(9, 0), LocalTime.of(18, 0), true, 0, null);
        pastNow = OffsetDateTime.parse("2026-01-01T00:00:00+09:00");
    }

    @Test
    void returnsSlotsCoveringBusinessHours() {
        List<SlotCalculator.AvailableSlot> slots = calculator.calculate(
                resource, List.of(openRule), List.of(), List.of(),
                MONDAY, 60, SEOUL, pastNow
        );
        assertThat(slots).hasSize(9);
        assertThat(slots.get(0).startAt().atZoneSameInstant(SEOUL).toLocalTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(slots.get(8).startAt().atZoneSameInstant(SEOUL).toLocalTime()).isEqualTo(LocalTime.of(17, 0));
    }

    @Test
    void returnsEmptyWhenNoRuleForDay() {
        AvailabilityRule tuesdayRule = new AvailabilityRule(null, 2, LocalTime.of(9, 0), LocalTime.of(18, 0), true, 0, null);
        List<SlotCalculator.AvailableSlot> slots = calculator.calculate(
                resource, List.of(tuesdayRule), List.of(), List.of(),
                MONDAY, 60, SEOUL, pastNow
        );
        assertThat(slots).isEmpty();
    }

    @Test
    void returnsEmptyWhenDayIsClosedByRule() {
        AvailabilityRule closed = new AvailabilityRule(null, 1, LocalTime.of(9, 0), LocalTime.of(18, 0), false, 0, null);
        List<SlotCalculator.AvailableSlot> slots = calculator.calculate(
                resource, List.of(closed), List.of(), List.of(),
                MONDAY, 60, SEOUL, pastNow
        );
        assertThat(slots).isEmpty();
    }

    @Test
    void filtersSlotsCoveredByBlackout() {
        BlackoutPeriod blackout = new BlackoutPeriod(null,
                OffsetDateTime.parse("2026-05-18T10:00:00+09:00"),
                OffsetDateTime.parse("2026-05-18T12:00:00+09:00"),
                "Maintenance");
        List<SlotCalculator.AvailableSlot> slots = calculator.calculate(
                resource, List.of(openRule), List.of(blackout), List.of(),
                MONDAY, 60, SEOUL, pastNow
        );
        assertThat(slots).hasSize(7);
        boolean anyBlockedSlot = slots.stream().anyMatch(s ->
                s.startAt().atZoneSameInstant(SEOUL).toLocalTime().equals(LocalTime.of(10, 0)) ||
                s.startAt().atZoneSameInstant(SEOUL).toLocalTime().equals(LocalTime.of(11, 0))
        );
        assertThat(anyBlockedSlot).isFalse();
    }

    @Test
    void filtersSlotsThatViolateLeadTime() {
        OffsetDateTime now = OffsetDateTime.parse("2026-05-18T09:30:00+09:00");
        AvailabilityRule rule = new AvailabilityRule(null, 1, LocalTime.of(9, 0), LocalTime.of(18, 0), true, 60, null);
        List<SlotCalculator.AvailableSlot> slots = calculator.calculate(
                resource, List.of(rule), List.of(), List.of(),
                MONDAY, 60, SEOUL, now
        );
        // 09:00 blocked (09:00 - 60min = 08:00 < now 09:30)
        // 10:00 blocked (10:00 - 60min = 09:00 < now 09:30)
        // 11:00 ok (11:00 - 60min = 10:00 > 09:30)
        assertThat(slots).hasSize(7);
        assertThat(slots.get(0).startAt().atZoneSameInstant(SEOUL).toLocalTime()).isEqualTo(LocalTime.of(11, 0));
    }

    @Test
    void returnsEmptyWhenSameDayCutoffPassed() {
        OffsetDateTime now = OffsetDateTime.parse("2026-05-18T10:00:00+09:00");
        AvailabilityRule rule = new AvailabilityRule(null, 1, LocalTime.of(9, 0), LocalTime.of(18, 0), true, 0, LocalTime.of(9, 30));
        List<SlotCalculator.AvailableSlot> slots = calculator.calculate(
                resource, List.of(rule), List.of(), List.of(),
                MONDAY, 60, SEOUL, now
        );
        assertThat(slots).isEmpty();
    }

    @Test
    void filtersSlotConflictingWithExistingReservationAndBuffer() {
        resource = stubResource(60, 60, 180, 15);
        Reservation existing = stubReservation(
                OffsetDateTime.parse("2026-05-18T10:00:00+09:00"),
                OffsetDateTime.parse("2026-05-18T11:00:00+09:00")
        );
        List<SlotCalculator.AvailableSlot> slots = calculator.calculate(
                resource, List.of(openRule), List.of(), List.of(existing),
                MONDAY, 60, SEOUL, pastNow
        );
        // buffer=15: 09:00 slot (window 08:45-10:15) conflicts with existing 10:00-11:00
        // 10:00 slot directly overlaps
        // 11:00 slot (window 10:45-12:15) conflicts with existing 10:00-11:00
        // 12:00-17:00 = 6 slots
        assertThat(slots).hasSize(6);
        assertThat(slots.get(0).startAt().atZoneSameInstant(SEOUL).toLocalTime()).isEqualTo(LocalTime.of(12, 0));
    }

    private Resource stubResource(int slotUnit, int minBooking, int maxBooking, int buffer) {
        return new Resource(null, null, null, "Test", 4, slotUnit, minBooking, maxBooking, buffer,
                BigDecimal.valueOf(10000), "KRW");
    }

    private Reservation stubReservation(OffsetDateTime start, OffsetDateTime end) {
        Reservation r = mock(Reservation.class);
        when(r.getStartAt()).thenReturn(start);
        when(r.getEndAt()).thenReturn(end);
        return r;
    }
}
