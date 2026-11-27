package com.jean325.slotops.location.api;

import com.jean325.slotops.location.application.LocationService;
import com.jean325.slotops.location.dto.CreateLocationRequest;
import com.jean325.slotops.location.dto.LocationResponse;
import com.jean325.slotops.location.dto.UpdateLocationRequest;
import com.jean325.slotops.global.config.ApiResponse;
import com.jean325.slotops.global.error.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import com.jean325.slotops.global.security.RequireOrgAccess;
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
@RequestMapping("/organizations/{organizationId}/locations")
@RequireOrgAccess
public class LocationController {
    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<LocationResponse>> createLocation(
        @PathVariable Long organizationId,
        @Valid @RequestBody CreateLocationRequest request,
        HttpServletRequest httpRequest
    ) {
        Long userId = getCurrentUserId(httpRequest);
        LocationResponse response = locationService.createLocation(organizationId, request, userId);
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.ok(response, "Location created successfully"));
    }

    @GetMapping("/{locationId}")
    public ResponseEntity<ApiResponse<LocationResponse>> getLocation(
        @PathVariable Long organizationId,
        @PathVariable Long locationId
    ) {
        LocationResponse response = locationService.getLocation(organizationId, locationId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<LocationResponse>>> listLocations(
        @PathVariable Long organizationId
    ) {
        List<LocationResponse> responses = locationService.listLocations(organizationId);
        return ResponseEntity.ok(ApiResponse.ok(responses));
    }

    @PutMapping("/{locationId}")
    public ResponseEntity<ApiResponse<LocationResponse>> updateLocation(
        @PathVariable Long organizationId,
        @PathVariable Long locationId,
        @RequestBody UpdateLocationRequest request
    ) {
        LocationResponse response = locationService.updateLocation(organizationId, locationId, request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Location updated successfully"));
    }

    @DeleteMapping("/{locationId}")
    public ResponseEntity<ApiResponse<Void>> deleteLocation(
        @PathVariable Long organizationId,
        @PathVariable Long locationId
    ) {
        locationService.deleteLocation(organizationId, locationId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Location deleted successfully"));
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        Object userIdObj = request.getAttribute("userId");
        if (userIdObj instanceof Long) {
            return (Long) userIdObj;
        }
        throw new ApiException(
            "UNAUTHORIZED",
            "User ID not found in request",
            HttpStatus.UNAUTHORIZED
        );
    }
}
