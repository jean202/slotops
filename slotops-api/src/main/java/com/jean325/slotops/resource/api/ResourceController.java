package com.jean325.slotops.resource.api;

import com.jean325.slotops.resource.application.ResourceService;
import com.jean325.slotops.resource.dto.CreateResourceRequest;
import com.jean325.slotops.resource.dto.ResourceResponse;
import com.jean325.slotops.resource.dto.UpdateResourceRequest;
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
@RequestMapping("/organizations/{organizationId}/locations/{locationId}/resources")
public class ResourceController {
    private final ResourceService resourceService;

    public ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ResourceResponse>> createResource(
        @PathVariable Long organizationId,
        @PathVariable Long locationId,
        @Valid @RequestBody CreateResourceRequest request
    ) {
        ResourceResponse response = resourceService.createResource(organizationId, locationId, request);
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.ok(response, "Resource created successfully"));
    }

    @GetMapping("/{resourceId}")
    public ResponseEntity<ApiResponse<ResourceResponse>> getResource(
        @PathVariable Long organizationId,
        @PathVariable Long locationId,
        @PathVariable Long resourceId
    ) {
        ResourceResponse response = resourceService.getResource(organizationId, locationId, resourceId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ResourceResponse>>> listResources(
        @PathVariable Long organizationId,
        @PathVariable Long locationId
    ) {
        List<ResourceResponse> responses = resourceService.listResources(organizationId, locationId);
        return ResponseEntity.ok(ApiResponse.ok(responses));
    }

    @PutMapping("/{resourceId}")
    public ResponseEntity<ApiResponse<ResourceResponse>> updateResource(
        @PathVariable Long organizationId,
        @PathVariable Long locationId,
        @PathVariable Long resourceId,
        @RequestBody UpdateResourceRequest request
    ) {
        ResourceResponse response = resourceService.updateResource(
            organizationId,
            locationId,
            resourceId,
            request
        );
        return ResponseEntity.ok(ApiResponse.ok(response, "Resource updated successfully"));
    }

    @DeleteMapping("/{resourceId}")
    public ResponseEntity<ApiResponse<Void>> deleteResource(
        @PathVariable Long organizationId,
        @PathVariable Long locationId,
        @PathVariable Long resourceId
    ) {
        resourceService.deleteResource(organizationId, locationId, resourceId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Resource deleted successfully"));
    }
}
