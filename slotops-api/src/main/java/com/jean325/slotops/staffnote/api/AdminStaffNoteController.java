package com.jean325.slotops.staffnote.api;

import com.jean325.slotops.global.config.ApiResponse;
import com.jean325.slotops.global.error.ApiException;
import com.jean325.slotops.global.security.RequireOrgAccess;
import com.jean325.slotops.staffnote.application.StaffNoteService;
import com.jean325.slotops.staffnote.dto.CreateStaffNoteRequest;
import com.jean325.slotops.staffnote.dto.StaffNoteResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/organizations/{orgId}/reservations/{reservationId}/notes")
@RequireOrgAccess
public class AdminStaffNoteController {

    private final StaffNoteService staffNoteService;

    public AdminStaffNoteController(StaffNoteService staffNoteService) {
        this.staffNoteService = staffNoteService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<StaffNoteResponse>> add(
            @PathVariable Long orgId,
            @PathVariable Long reservationId,
            @Valid @RequestBody CreateStaffNoteRequest request,
            HttpServletRequest httpRequest
    ) {
        Long userId = getCurrentUserId(httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(staffNoteService.add(orgId, reservationId, userId, request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<StaffNoteResponse>>> list(
            @PathVariable Long orgId,
            @PathVariable Long reservationId,
            HttpServletRequest httpRequest
    ) {
        Long userId = getCurrentUserId(httpRequest);
        return ResponseEntity.ok(ApiResponse.ok(staffNoteService.list(orgId, reservationId, userId)));
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        Object userIdObj = request.getAttribute("userId");
        if (userIdObj instanceof Long) {
            return (Long) userIdObj;
        }
        throw new ApiException("UNAUTHORIZED", "User ID not found in request", HttpStatus.UNAUTHORIZED);
    }
}
