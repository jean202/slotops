package com.jean325.slotops.availability.api;

import com.jean325.slotops.availability.application.AvailabilityService;
import com.jean325.slotops.availability.dto.AvailabilityRuleResponse;
import com.jean325.slotops.availability.dto.BlackoutPeriodResponse;
import com.jean325.slotops.availability.dto.CreateAvailabilityRuleRequest;
import com.jean325.slotops.availability.dto.CreateBlackoutPeriodRequest;
import com.jean325.slotops.global.config.ApiResponse;
import com.jean325.slotops.global.error.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/organizations/{orgId}/resources/{resourceId}")
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    public AvailabilityController(AvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    @PostMapping("/availability-rules")
    public ResponseEntity<ApiResponse<AvailabilityRuleResponse>> createRule(
            @PathVariable Long orgId,
            @PathVariable Long resourceId,
            @Valid @RequestBody CreateAvailabilityRuleRequest request,
            HttpServletRequest httpRequest
    ) {
        Long userId = getCurrentUserId(httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(availabilityService.createRule(orgId, resourceId, userId, request)));
    }

    @GetMapping("/availability-rules")
    public ResponseEntity<ApiResponse<List<AvailabilityRuleResponse>>> listRules(
            @PathVariable Long orgId,
            @PathVariable Long resourceId,
            HttpServletRequest httpRequest
    ) {
        Long userId = getCurrentUserId(httpRequest);
        return ResponseEntity.ok(ApiResponse.ok(availabilityService.listRules(orgId, resourceId, userId)));
    }

    @PutMapping("/availability-rules/{ruleId}")
    public ResponseEntity<ApiResponse<AvailabilityRuleResponse>> updateRule(
            @PathVariable Long orgId,
            @PathVariable Long resourceId,
            @PathVariable Long ruleId,
            @Valid @RequestBody CreateAvailabilityRuleRequest request,
            HttpServletRequest httpRequest
    ) {
        Long userId = getCurrentUserId(httpRequest);
        return ResponseEntity.ok(ApiResponse.ok(availabilityService.updateRule(orgId, resourceId, ruleId, userId, request)));
    }

    @DeleteMapping("/availability-rules/{ruleId}")
    public ResponseEntity<ApiResponse<Void>> deleteRule(
            @PathVariable Long orgId,
            @PathVariable Long resourceId,
            @PathVariable Long ruleId,
            HttpServletRequest httpRequest
    ) {
        Long userId = getCurrentUserId(httpRequest);
        availabilityService.deleteRule(orgId, resourceId, ruleId, userId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/blackout-periods")
    public ResponseEntity<ApiResponse<BlackoutPeriodResponse>> createBlackout(
            @PathVariable Long orgId,
            @PathVariable Long resourceId,
            @Valid @RequestBody CreateBlackoutPeriodRequest request,
            HttpServletRequest httpRequest
    ) {
        Long userId = getCurrentUserId(httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(availabilityService.createBlackout(orgId, resourceId, userId, request)));
    }

    @GetMapping("/blackout-periods")
    public ResponseEntity<ApiResponse<List<BlackoutPeriodResponse>>> listBlackouts(
            @PathVariable Long orgId,
            @PathVariable Long resourceId,
            HttpServletRequest httpRequest
    ) {
        Long userId = getCurrentUserId(httpRequest);
        return ResponseEntity.ok(ApiResponse.ok(availabilityService.listBlackouts(orgId, resourceId, userId)));
    }

    @DeleteMapping("/blackout-periods/{periodId}")
    public ResponseEntity<ApiResponse<Void>> deleteBlackout(
            @PathVariable Long orgId,
            @PathVariable Long resourceId,
            @PathVariable Long periodId,
            HttpServletRequest httpRequest
    ) {
        Long userId = getCurrentUserId(httpRequest);
        availabilityService.deleteBlackout(orgId, resourceId, periodId, userId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        Object userIdObj = request.getAttribute("userId");
        if (userIdObj instanceof Long) {
            return (Long) userIdObj;
        }
        throw new ApiException("UNAUTHORIZED", "User ID not found in request", HttpStatus.UNAUTHORIZED);
    }
}
