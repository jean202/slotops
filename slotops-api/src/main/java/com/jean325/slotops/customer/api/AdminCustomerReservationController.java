package com.jean325.slotops.customer.api;

import com.jean325.slotops.global.config.ApiResponse;
import com.jean325.slotops.global.config.PageResponse;
import com.jean325.slotops.global.error.ApiException;
import com.jean325.slotops.reservation.application.ReservationService;
import com.jean325.slotops.reservation.dto.AdminReservationResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/organizations/{orgId}/customers/{customerId}/reservations")
public class AdminCustomerReservationController {

    private final ReservationService reservationService;

    public AdminCustomerReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AdminReservationResponse>>> list(
            @PathVariable Long orgId,
            @PathVariable Long customerId,
            @PageableDefault(size = 20, sort = "startAt", direction = Sort.Direction.DESC) Pageable pageable,
            HttpServletRequest httpRequest
    ) {
        Long userId = getCurrentUserId(httpRequest);
        return ResponseEntity.ok(ApiResponse.ok(
                reservationService.listForCustomer(orgId, customerId, userId, pageable)));
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        Object userIdObj = request.getAttribute("userId");
        if (userIdObj instanceof Long) {
            return (Long) userIdObj;
        }
        throw new ApiException("UNAUTHORIZED", "User ID not found in request", HttpStatus.UNAUTHORIZED);
    }
}
