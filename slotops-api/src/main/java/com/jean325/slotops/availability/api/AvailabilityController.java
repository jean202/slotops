package com.jean325.slotops.availability.api;

import com.jean325.slotops.availability.application.AvailabilityService;
import com.jean325.slotops.availability.dto.AvailabilityRuleResponse;
import com.jean325.slotops.availability.dto.BlackoutPeriodResponse;
import com.jean325.slotops.availability.dto.CreateAvailabilityRuleRequest;
import com.jean325.slotops.availability.dto.CreateBlackoutPeriodRequest;
import com.jean325.slotops.global.config.ApiResponse;
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
            @Valid @RequestBody CreateAvailabilityRuleRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(availabilityService.createRule(orgId, resourceId, request)));
    }

    @GetMapping("/availability-rules")
    public ResponseEntity<ApiResponse<List<AvailabilityRuleResponse>>> listRules(
            @PathVariable Long orgId,
            @PathVariable Long resourceId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(availabilityService.listRules(orgId, resourceId)));
    }

    @PutMapping("/availability-rules/{ruleId}")
    public ResponseEntity<ApiResponse<AvailabilityRuleResponse>> updateRule(
            @PathVariable Long orgId,
            @PathVariable Long resourceId,
            @PathVariable Long ruleId,
            @Valid @RequestBody CreateAvailabilityRuleRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(availabilityService.updateRule(orgId, resourceId, ruleId, request)));
    }

    @DeleteMapping("/availability-rules/{ruleId}")
    public ResponseEntity<ApiResponse<Void>> deleteRule(
            @PathVariable Long orgId,
            @PathVariable Long resourceId,
            @PathVariable Long ruleId
    ) {
        availabilityService.deleteRule(orgId, resourceId, ruleId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/blackout-periods")
    public ResponseEntity<ApiResponse<BlackoutPeriodResponse>> createBlackout(
            @PathVariable Long orgId,
            @PathVariable Long resourceId,
            @Valid @RequestBody CreateBlackoutPeriodRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(availabilityService.createBlackout(orgId, resourceId, request)));
    }

    @GetMapping("/blackout-periods")
    public ResponseEntity<ApiResponse<List<BlackoutPeriodResponse>>> listBlackouts(
            @PathVariable Long orgId,
            @PathVariable Long resourceId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(availabilityService.listBlackouts(orgId, resourceId)));
    }

    @DeleteMapping("/blackout-periods/{periodId}")
    public ResponseEntity<ApiResponse<Void>> deleteBlackout(
            @PathVariable Long orgId,
            @PathVariable Long resourceId,
            @PathVariable Long periodId
    ) {
        availabilityService.deleteBlackout(orgId, resourceId, periodId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
