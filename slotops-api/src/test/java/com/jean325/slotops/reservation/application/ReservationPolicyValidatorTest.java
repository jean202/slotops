package com.jean325.slotops.reservation.application;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jean325.slotops.global.error.ApiException;
import com.jean325.slotops.resource.domain.Resource;
import com.jean325.slotops.resource.domain.ResourceType;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ReservationPolicyValidatorTest {

    private final ReservationPolicyValidator validator = new ReservationPolicyValidator();
    private final OffsetDateTime start = OffsetDateTime.parse("2026-06-15T10:00:00+09:00");

    // capacity 4, 30-minute slots, bookings of 60..180 minutes
    private final Resource resource = new Resource(
            null, null, ResourceType.ROOM, "Room", 4, 30, 60, 180, 0, new BigDecimal("10000"), "KRW");

    private void assertRejected(ThrowingCallable call, String code) {
        assertThatThrownBy(call)
                .isInstanceOfSatisfying(ApiException.class, ex -> {
                    org.assertj.core.api.Assertions.assertThat(ex.getCode()).isEqualTo(code);
                    org.assertj.core.api.Assertions.assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                });
    }

    @Test
    void acceptsBookingWithinAllLimits() {
        assertThatCode(() -> validator.validate(resource, start, start.plusMinutes(90), 2))
                .doesNotThrowAnyException();
    }

    @Test
    void acceptsBoundaryDurationsAndFullCapacity() {
        assertThatCode(() -> validator.validate(resource, start, start.plusMinutes(60), 4))
                .doesNotThrowAnyException();
        assertThatCode(() -> validator.validate(resource, start, start.plusMinutes(180), 1))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsMissingOrNonPositiveGuestCount() {
        assertRejected(() -> validator.validate(resource, start, start.plusMinutes(60), null),
                "RESERVATION_INVALID_GUEST_COUNT");
        assertRejected(() -> validator.validate(resource, start, start.plusMinutes(60), 0),
                "RESERVATION_INVALID_GUEST_COUNT");
    }

    @Test
    void rejectsEndNotAfterStart() {
        assertRejected(() -> validator.validate(resource, start, start, 2),
                "RESERVATION_INVALID_TIME_RANGE");
        assertRejected(() -> validator.validate(resource, start, start.minusMinutes(60), 2),
                "RESERVATION_INVALID_TIME_RANGE");
    }

    @Test
    void rejectsDurationShorterThanMinimum() {
        assertRejected(() -> validator.validate(resource, start, start.plusMinutes(30), 2),
                "RESERVATION_DURATION_OUT_OF_RANGE");
    }

    @Test
    void rejectsDurationLongerThanMaximum() {
        assertRejected(() -> validator.validate(resource, start, start.plusMinutes(210), 2),
                "RESERVATION_DURATION_OUT_OF_RANGE");
    }

    @Test
    void rejectsDurationNotAlignedToSlotUnit() {
        assertRejected(() -> validator.validate(resource, start, start.plusMinutes(75), 2),
                "RESERVATION_INVALID_SLOT_UNIT");
    }

    @Test
    void rejectsGuestCountAboveCapacity() {
        assertRejected(() -> validator.validate(resource, start, start.plusMinutes(60), 5),
                "RESERVATION_CAPACITY_EXCEEDED");
    }

    @Test
    void measuresDurationAcrossDifferentOffsets() {
        // 10:00+09:00 to 02:30Z is 90 minutes of real time.
        OffsetDateTime endUtc = OffsetDateTime.parse("2026-06-15T02:30:00Z");

        assertThatCode(() -> validator.validate(resource, start, endUtc, 2)).doesNotThrowAnyException();
    }
}
