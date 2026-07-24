package com.jean325.slotops.addon.application;

import com.jean325.slotops.addon.domain.Addon;
import com.jean325.slotops.addon.domain.AddonRepository;
import com.jean325.slotops.addon.dto.AddonResponse;
import com.jean325.slotops.addon.dto.CreateAddonRequest;
import com.jean325.slotops.global.error.ApiException;
import com.jean325.slotops.organization.domain.OrganizationMemberRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AddonService {

    private final AddonRepository addonRepository;
    private final OrganizationMemberRepository organizationMemberRepository;

    public AddonService(
            AddonRepository addonRepository,
            OrganizationMemberRepository organizationMemberRepository
    ) {
        this.addonRepository = addonRepository;
        this.organizationMemberRepository = organizationMemberRepository;
    }

    @Transactional
    public AddonResponse create(Long orgId, Long userId, CreateAddonRequest request) {
        requireOrgMembership(orgId, userId);
        Addon addon = addonRepository.save(new Addon(orgId, request.name(), request.price()));
        return toResponse(addon);
    }

    @Transactional(readOnly = true)
    public List<AddonResponse> list(Long orgId, Long userId) {
        requireOrgMembership(orgId, userId);
        return addonRepository.findByOrganizationIdOrderByNameAsc(orgId).stream()
                .map(this::toResponse)
                .toList();
    }

    private void requireOrgMembership(Long orgId, Long userId) {
        if (!organizationMemberRepository.existsByOrganizationIdAndUserId(orgId, userId)) {
            throw new ApiException("FORBIDDEN",
                    "User is not a member of this organization", HttpStatus.FORBIDDEN);
        }
    }

    private AddonResponse toResponse(Addon addon) {
        return new AddonResponse(addon.getId(), addon.getName(), addon.getPrice(), addon.getStatus().name());
    }
}
