package com.jean325.slotops.dashboard.api;

import com.jean325.slotops.dashboard.application.DashboardService;
import com.jean325.slotops.dashboard.dto.DashboardSummaryResponse;
import com.jean325.slotops.global.config.ApiResponse;
import com.jean325.slotops.global.error.ApiException;
import com.jean325.slotops.global.security.RequireOrgAccess;
import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/organizations/{orgId}/dashboard")
@RequireOrgAccess
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<DashboardSummaryResponse>> summary(
            @PathVariable Long orgId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            HttpServletRequest httpRequest
    ) {
        Long userId = getCurrentUserId(httpRequest);
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.summary(orgId, userId, from, to)));
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        Object userIdObj = request.getAttribute("userId");
        if (userIdObj instanceof Long) {
            return (Long) userIdObj;
        }
        throw new ApiException("UNAUTHORIZED", "User ID not found in request", HttpStatus.UNAUTHORIZED);
    }
}
