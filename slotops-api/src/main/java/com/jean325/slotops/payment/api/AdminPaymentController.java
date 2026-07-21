package com.jean325.slotops.payment.api;

import com.jean325.slotops.global.config.ApiResponse;
import com.jean325.slotops.global.error.ApiException;
import com.jean325.slotops.payment.application.PaymentService;
import com.jean325.slotops.payment.dto.PaymentResponse;
import com.jean325.slotops.payment.dto.UpdatePaymentRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/organizations/{orgId}/reservations/{reservationId}/payment")
public class AdminPaymentController {

    private final PaymentService paymentService;

    public AdminPaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PaymentResponse>> get(
            @PathVariable Long orgId,
            @PathVariable Long reservationId,
            HttpServletRequest httpRequest
    ) {
        Long userId = getCurrentUserId(httpRequest);
        return ResponseEntity.ok(ApiResponse.ok(paymentService.getForReservation(orgId, reservationId, userId)));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<PaymentResponse>> update(
            @PathVariable Long orgId,
            @PathVariable Long reservationId,
            @Valid @RequestBody UpdatePaymentRequest request,
            HttpServletRequest httpRequest
    ) {
        Long userId = getCurrentUserId(httpRequest);
        return ResponseEntity.ok(ApiResponse.ok(paymentService.updateStatus(orgId, reservationId, userId, request)));
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        Object userIdObj = request.getAttribute("userId");
        if (userIdObj instanceof Long) {
            return (Long) userIdObj;
        }
        throw new ApiException("UNAUTHORIZED", "User ID not found in request", HttpStatus.UNAUTHORIZED);
    }
}
