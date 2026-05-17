package com.jean325.slotops.availability.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.jean325.slotops.reservation.domain.Reservation;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReservationConflictCheckerTest {

    private ReservationConflictChecker checker;

    @BeforeEach
    void setUp() {
        checker = new ReservationConflictChecker();
    }

    private Reservation stubReservation(String start, String end) {
        Reservation r = mock(Reservation.class);
        when(r.getStartAt()).thenReturn(odt(start));
        when(r.getEndAt()).thenReturn(odt(end));
        return r;
    }

    private OffsetDateTime odt(String time) {
        return OffsetDateTime.parse("2026-05-19T" + time + ":00+09:00");
    }

    @Test
    void returnsFalseWhenNoReservations() {
        assertThat(checker.hasConflict(List.of(), odt("10:00"), odt("11:00"), 0)).isFalse();
    }

    @Test
    void returnsFalseWhenAdjacentNoBuffer() {
        assertThat(checker.hasConflict(
                List.of(stubReservation("09:00", "10:00")),
                odt("10:00"), odt("11:00"), 0
        )).isFalse();
    }

    @Test
    void returnsTrueWhenOverlapping() {
        assertThat(checker.hasConflict(
                List.of(stubReservation("09:00", "11:00")),
                odt("10:00"), odt("12:00"), 0
        )).isTrue();
    }

    @Test
    void returnsTrueWhenAdjacentWithBuffer() {
        assertThat(checker.hasConflict(
                List.of(stubReservation("09:00", "10:00")),
                odt("10:00"), odt("11:00"), 15
        )).isTrue();
    }

    @Test
    void returnsFalseWhenOutsideBuffer() {
        assertThat(checker.hasConflict(
                List.of(stubReservation("09:00", "10:00")),
                odt("10:30"), odt("11:30"), 15
        )).isFalse();
    }
}
