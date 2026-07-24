package com.jean325.slotops.global.security;

import com.jean325.slotops.global.error.ApiException;
import com.jean325.slotops.organization.domain.OrganizationMemberRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Single source of the organization-membership check. Used both by the
 * {@link OrgAccessAspect} at the controller boundary and by service methods
 * for defense in depth close to data access.
 */
@Component
public class OrgAccessGuard {

    private final OrganizationMemberRepository organizationMemberRepository;

    public OrgAccessGuard(OrganizationMemberRepository organizationMemberRepository) {
        this.organizationMemberRepository = organizationMemberRepository;
    }

    public void requireMembership(Long orgId, Long userId) {
        if (!organizationMemberRepository.existsByOrganizationIdAndUserId(orgId, userId)) {
            throw new ApiException("FORBIDDEN",
                    "User is not a member of this organization", HttpStatus.FORBIDDEN);
        }
    }
}
