package com.jean325.slotops.addon.application;

import com.jean325.slotops.addon.domain.Addon;
import com.jean325.slotops.addon.domain.AddonRepository;
import com.jean325.slotops.addon.dto.AddonResponse;
import com.jean325.slotops.addon.dto.CreateAddonRequest;
import com.jean325.slotops.global.security.OrgAccessGuard;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AddonService {

    private final AddonRepository addonRepository;
    private final OrgAccessGuard orgAccessGuard;

    public AddonService(
            AddonRepository addonRepository,
            OrgAccessGuard orgAccessGuard
    ) {
        this.addonRepository = addonRepository;
        this.orgAccessGuard = orgAccessGuard;
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
        orgAccessGuard.requireMembership(orgId, userId);
    }

    private AddonResponse toResponse(Addon addon) {
        return new AddonResponse(addon.getId(), addon.getName(), addon.getPrice(), addon.getStatus().name());
    }
}
