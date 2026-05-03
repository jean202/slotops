package com.jean325.slotops.organization.api;

import com.jean325.slotops.organization.application.OrganizationService;
import com.jean325.slotops.organization.dto.AddMemberRequest;
import com.jean325.slotops.organization.dto.CreateOrganizationRequest;
import com.jean325.slotops.organization.dto.OrganizationMemberResponse;
import com.jean325.slotops.organization.dto.OrganizationResponse;
import com.jean325.slotops.global.config.ApiResponse;
import com.jean325.slotops.global.error.ApiException;
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
@RequestMapping("/organizations")
public class OrganizationController {
    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<OrganizationResponse>> createOrganization(
        @Valid @RequestBody CreateOrganizationRequest request,
        HttpServletRequest httpRequest
    ) {
        Long currentUserId = getCurrentUserId(httpRequest);
        OrganizationResponse response = organizationService.createOrganization(request, currentUserId);
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.ok(response, "Organization created successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrganizationResponse>> getOrganization(
        @PathVariable Long id,
        HttpServletRequest httpRequest
    ) {
        Long currentUserId = getCurrentUserId(httpRequest);
        OrganizationResponse response = organizationService.getOrganization(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrganizationResponse>>> getMyOrganizations(
        HttpServletRequest httpRequest
    ) {
        Long currentUserId = getCurrentUserId(httpRequest);
        List<OrganizationResponse> responses = organizationService.getUserOrganizations(currentUserId);
        return ResponseEntity.ok(ApiResponse.ok(responses));
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<ApiResponse<OrganizationMemberResponse>> addMember(
        @PathVariable Long id,
        @Valid @RequestBody AddMemberRequest request,
        HttpServletRequest httpRequest
    ) {
        Long currentUserId = getCurrentUserId(httpRequest);
        OrganizationMemberResponse response = organizationService.addMember(id, request, currentUserId);
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.ok(response, "Member added successfully"));
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<ApiResponse<List<OrganizationMemberResponse>>> getMembers(
        @PathVariable Long id,
        HttpServletRequest httpRequest
    ) {
        Long currentUserId = getCurrentUserId(httpRequest);
        List<OrganizationMemberResponse> responses = organizationService.getMembers(id, currentUserId);
        return ResponseEntity.ok(ApiResponse.ok(responses));
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
