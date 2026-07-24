package com.jean325.slotops.reservation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record PublicCreateReservationRequest(
        @NotNull Long resourceId,
        @NotNull LocalDate date,
        @NotNull LocalTime startTime,
        @NotNull @Min(30) @Max(1440) Integer durationMinutes,
        @NotNull @Min(1) @Max(100) Integer guestCount,
        @NotNull @Valid CustomerRequest customer,
        @Size(max = 500) String specialRequest,
        @Valid List<AddonLine> addons
) {
    public record CustomerRequest(
            @NotBlank @Size(max = 100) String name,
            @Email @Size(max = 150) String email,
            @NotBlank @Size(max = 30) String phone
    ) {
    }

    public record AddonLine(
            @NotNull Long addonId,
            @NotNull @Min(1) @Max(100) Integer quantity
    ) {
    }
}
