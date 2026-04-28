package com.jean325.slotops.reservation.application;

import com.jean325.slotops.global.error.ApiException;
import com.jean325.slotops.resource.domain.Resource;
import java.time.Duration;
import java.time.OffsetDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class ReservationPolicyValidator {

    public void validate(Resource resource, OffsetDateTime startAt, OffsetDateTime endAt, Integer guestCount) {
        if (!startAt.isBefore(endAt)) {
            throw new ApiException(
                    "RESERVATION_INVALID_TIME_RANGE",
                    "Reservation start time must be earlier than the end time.",
                    HttpStatus.BAD_REQUEST
            );
        }

        long durationMinutes = Duration.between(startAt, endAt).toMinutes();
        if (durationMinutes < resource.getMinBookingMinutes() || durationMinutes > resource.getMaxBookingMinutes()) {
            throw new ApiException(
                    "RESERVATION_DURATION_OUT_OF_RANGE",
                    "Reservation duration is outside the allowed range for this resource.",
                    HttpStatus.BAD_REQUEST
            );
        }

        if (durationMinutes % resource.getSlotUnitMinutes() != 0) {
            throw new ApiException(
                    "RESERVATION_INVALID_SLOT_UNIT",
                    "Reservation duration must align with the resource slot unit.",
                    HttpStatus.BAD_REQUEST
            );
        }

        if (guestCount > resource.getCapacity()) {
            throw new ApiException(
                    "RESERVATION_CAPACITY_EXCEEDED",
                    "Guest count exceeds the resource capacity.",
                    HttpStatus.BAD_REQUEST
            );
        }
    }
}
