package com.jean325.slotops.reservation.api;

import com.jean325.slotops.reservation.application.ReservationService;
import com.jean325.slotops.reservation.dto.PublicCreateReservationRequest;
import com.jean325.slotops.reservation.dto.PublicReservationResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/reservations")
public class PublicReservationController {

    private final ReservationService reservationService;

    public PublicReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PublicReservationResponse createReservation(@Valid @RequestBody PublicCreateReservationRequest request) {
        return reservationService.createPublicReservation(request);
    }
}
