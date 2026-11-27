package com.jean325.slotops.global.security;

import com.jean325.slotops.global.error.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.HandlerMapping;

/**
 * Enforces {@link RequireOrgAccess} at the controller boundary: reads the
 * authenticated userId (set by the JWT filter) and the {@code orgId} (or
 * {@code organizationId}) path variable, then delegates to {@link OrgAccessGuard}.
 */
@Aspect
@Component
public class OrgAccessAspect {

    private final OrgAccessGuard orgAccessGuard;

    public OrgAccessAspect(OrgAccessGuard orgAccessGuard) {
        this.orgAccessGuard = orgAccessGuard;
    }

    @Before("@within(com.jean325.slotops.global.security.RequireOrgAccess) "
            + "|| @annotation(com.jean325.slotops.global.security.RequireOrgAccess)")
    public void checkOrgAccess() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new ApiException("UNAUTHORIZED", "No request context available.", HttpStatus.UNAUTHORIZED);
        }
        HttpServletRequest request = attributes.getRequest();

        if (!(request.getAttribute("userId") instanceof Long userId)) {
            throw new ApiException("UNAUTHORIZED", "User ID not found in request", HttpStatus.UNAUTHORIZED);
        }

        Long orgId = extractOrgId(request);
        orgAccessGuard.requireMembership(orgId, userId);
    }

    @SuppressWarnings("unchecked")
    private Long extractOrgId(HttpServletRequest request) {
        Object vars = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        Map<String, String> pathVars = vars instanceof Map<?, ?> ? (Map<String, String>) vars : Map.of();
        String orgId = pathVars.containsKey("orgId") ? pathVars.get("orgId") : pathVars.get("organizationId");
        if (orgId == null) {
            throw new ApiException("UNAUTHORIZED", "Organization not resolved from request path.",
                    HttpStatus.UNAUTHORIZED);
        }
        return Long.valueOf(orgId);
    }
}
