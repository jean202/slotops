package com.jean325.slotops.addon.api;

import com.jean325.slotops.addon.application.AddonService;
import com.jean325.slotops.addon.dto.AddonResponse;
import com.jean325.slotops.addon.dto.CreateAddonRequest;
import com.jean325.slotops.global.config.ApiResponse;
import com.jean325.slotops.global.error.ApiException;
import com.jean325.slotops.global.security.RequireOrgAccess;
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
@RequestMapping("/api/v1/organizations/{orgId}/addons")
@RequireOrgAccess
public class AdminAddonController {

    private final AddonService addonService;

    public AdminAddonController(AddonService addonService) {
        this.addonService = addonService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AddonResponse>> create(
            @PathVariable Long orgId,
            @Valid @RequestBody CreateAddonRequest request,
            HttpServletRequest httpRequest
    ) {
        Long userId = getCurrentUserId(httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(addonService.create(orgId, userId, request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AddonResponse>>> list(
            @PathVariable Long orgId,
            HttpServletRequest httpRequest
    ) {
        Long userId = getCurrentUserId(httpRequest);
        return ResponseEntity.ok(ApiResponse.ok(addonService.list(orgId, userId)));
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        Object userIdObj = request.getAttribute("userId");
        if (userIdObj instanceof Long) {
            return (Long) userIdObj;
        }
        throw new ApiException("UNAUTHORIZED", "User ID not found in request", HttpStatus.UNAUTHORIZED);
    }
}
