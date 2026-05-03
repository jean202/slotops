package com.jean325.slotops.organization.application;

import com.jean325.slotops.organization.domain.Organization;
import com.jean325.slotops.organization.domain.OrganizationMember;
import com.jean325.slotops.organization.domain.OrganizationMemberRepository;
import com.jean325.slotops.organization.domain.OrganizationMemberRole;
import com.jean325.slotops.organization.domain.OrganizationRepository;
import com.jean325.slotops.organization.dto.AddMemberRequest;
import com.jean325.slotops.organization.dto.CreateOrganizationRequest;
import com.jean325.slotops.organization.dto.OrganizationMemberResponse;
import com.jean325.slotops.organization.dto.OrganizationResponse;
import com.jean325.slotops.global.error.ApiException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class OrganizationService {
    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository memberRepository;

    public OrganizationService(
        OrganizationRepository organizationRepository,
        OrganizationMemberRepository memberRepository
    ) {
        this.organizationRepository = organizationRepository;
        this.memberRepository = memberRepository;
    }

    public OrganizationResponse createOrganization(CreateOrganizationRequest request, Long ownerUserId) {
        if (organizationRepository.findBySlug(request.slug()).isPresent()) {
            throw new ApiException(
                "SLUG_ALREADY_EXISTS",
                "Organization slug already exists",
                HttpStatus.CONFLICT
            );
        }

        Organization org = new Organization(request.name(), request.slug(), ownerUserId);
        Organization savedOrg = organizationRepository.save(org);

        // Add owner as OWNER member
        OrganizationMember ownerMember = new OrganizationMember(
            savedOrg,
            ownerUserId,
            OrganizationMemberRole.OWNER
        );
        memberRepository.save(ownerMember);

        return new OrganizationResponse(
            savedOrg.getId(),
            savedOrg.getName(),
            savedOrg.getSlug(),
            savedOrg.getOwnerUserId(),
            savedOrg.getStatus(),
            savedOrg.getCreatedAt(),
            savedOrg.getUpdatedAt()
        );
    }

    public OrganizationResponse getOrganization(Long organizationId, Long currentUserId) {
        Organization org = organizationRepository.findById(organizationId)
            .orElseThrow(() -> new ApiException(
                "ORGANIZATION_NOT_FOUND",
                "Organization not found",
                HttpStatus.NOT_FOUND
            ));

        // Check if user has access to this organization
        memberRepository.findByOrganizationIdAndUserId(organizationId, currentUserId)
            .orElseThrow(() -> new ApiException(
                "FORBIDDEN",
                "Access denied to this organization",
                HttpStatus.FORBIDDEN
            ));

        return new OrganizationResponse(
            org.getId(),
            org.getName(),
            org.getSlug(),
            org.getOwnerUserId(),
            org.getStatus(),
            org.getCreatedAt(),
            org.getUpdatedAt()
        );
    }

    public OrganizationMemberResponse addMember(
        Long organizationId,
        AddMemberRequest request,
        Long currentUserId
    ) {
        Organization org = organizationRepository.findById(organizationId)
            .orElseThrow(() -> new ApiException(
                "ORGANIZATION_NOT_FOUND",
                "Organization not found",
                HttpStatus.NOT_FOUND
            ));

        // Check if current user is OWNER or MANAGER
        OrganizationMember currentMember = memberRepository
            .findByOrganizationIdAndUserId(organizationId, currentUserId)
            .orElseThrow(() -> new ApiException(
                "FORBIDDEN",
                "You don't have permission to add members",
                HttpStatus.FORBIDDEN
            ));

        if (currentMember.getRole() != OrganizationMemberRole.OWNER &&
            currentMember.getRole() != OrganizationMemberRole.MANAGER) {
            throw new ApiException(
                "FORBIDDEN",
                "Only OWNER or MANAGER can add members",
                HttpStatus.FORBIDDEN
            );
        }

        // Check if user is already a member
        if (memberRepository.findByOrganizationIdAndUserId(organizationId, request.userId()).isPresent()) {
            throw new ApiException(
                "MEMBER_ALREADY_EXISTS",
                "User is already a member of this organization",
                HttpStatus.CONFLICT
            );
        }

        OrganizationMember newMember = new OrganizationMember(org, request.userId(), request.role());
        OrganizationMember savedMember = memberRepository.save(newMember);

        return new OrganizationMemberResponse(
            savedMember.getId(),
            savedMember.getOrganization().getId(),
            savedMember.getUserId(),
            savedMember.getRole(),
            savedMember.getJoinedAt()
        );
    }

    public List<OrganizationMemberResponse> getMembers(Long organizationId, Long currentUserId) {
        // Check if user has access
        memberRepository.findByOrganizationIdAndUserId(organizationId, currentUserId)
            .orElseThrow(() -> new ApiException(
                "FORBIDDEN",
                "Access denied to this organization",
                HttpStatus.FORBIDDEN
            ));

        return memberRepository.findByOrganizationId(organizationId)
            .stream()
            .map(m -> new OrganizationMemberResponse(
                m.getId(),
                m.getOrganization().getId(),
                m.getUserId(),
                m.getRole(),
                m.getJoinedAt()
            ))
            .toList();
    }

    public List<OrganizationResponse> getUserOrganizations(Long userId) {
        return memberRepository.findByUserId(userId)
            .stream()
            .map(m -> new OrganizationResponse(
                m.getOrganization().getId(),
                m.getOrganization().getName(),
                m.getOrganization().getSlug(),
                m.getOrganization().getOwnerUserId(),
                m.getOrganization().getStatus(),
                m.getOrganization().getCreatedAt(),
                m.getOrganization().getUpdatedAt()
            ))
            .toList();
    }
}
