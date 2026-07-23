package com.jean325.slotops.reservation.api;

import com.jean325.slotops.global.config.ApiResponse;
import com.jean325.slotops.global.config.PageResponse;
import com.jean325.slotops.global.error.ApiException;
import com.jean325.slotops.reservation.application.ReservationService;
import com.jean325.slotops.reservation.domain.ReservationStatus;
import com.jean325.slotops.reservation.dto.AdminReservationResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/organizations/{orgId}/reservations")
public class AdminReservationController {

    private final ReservationService reservationService;

    public AdminReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AdminReservationResponse>>> list(
            @PathVariable Long orgId,
            @RequestParam(required = false) ReservationStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(required = false) Long customerId,
            @PageableDefault(size = 20, sort = "startAt", direction = Sort.Direction.DESC) Pageable pageable,
            HttpServletRequest httpRequest
    ) {
        Long userId = getCurrentUserId(httpRequest);
        return ResponseEntity.ok(ApiResponse.ok(
                reservationService.listForOrg(orgId, userId, status, from, to, customerId, pageable)));
    }

    @PostMapping("/{reservationId}/confirm")
    public ResponseEntity<ApiResponse<AdminReservationResponse>> confirm(
            @PathVariable Long orgId,
            @PathVariable Long reservationId,
            HttpServletRequest httpRequest
    ) {
        Long userId = getCurrentUserId(httpRequest);
        return ResponseEntity.ok(ApiResponse.ok(reservationService.confirm(orgId, reservationId, userId)));
    }

    @PostMapping("/{reservationId}/cancel")
    public ResponseEntity<ApiResponse<AdminReservationResponse>> cancel(
            @PathVariable Long orgId,
            @PathVariable Long reservationId,
            HttpServletRequest httpRequest
    ) {
        Long userId = getCurrentUserId(httpRequest);
        return ResponseEntity.ok(ApiResponse.ok(reservationService.cancel(orgId, reservationId, userId)));
    }

    @PostMapping("/{reservationId}/complete")
    public ResponseEntity<ApiResponse<AdminReservationResponse>> complete(
            @PathVariable Long orgId,
            @PathVariable Long reservationId,
            HttpServletRequest httpRequest
    ) {
        Long userId = getCurrentUserId(httpRequest);
        return ResponseEntity.ok(ApiResponse.ok(reservationService.complete(orgId, reservationId, userId)));
    }

    @PostMapping("/{reservationId}/no-show")
    public ResponseEntity<ApiResponse<AdminReservationResponse>> noShow(
            @PathVariable Long orgId,
            @PathVariable Long reservationId,
            HttpServletRequest httpRequest
    ) {
        Long userId = getCurrentUserId(httpRequest);
        return ResponseEntity.ok(ApiResponse.ok(reservationService.markNoShow(orgId, reservationId, userId)));
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        Object userIdObj = request.getAttribute("userId");
        if (userIdObj instanceof Long) {
            return (Long) userIdObj;
        }
        throw new ApiException("UNAUTHORIZED", "User ID not found in request", HttpStatus.UNAUTHORIZED);
    }
}
