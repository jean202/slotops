package com.jean325.slotops.global.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a controller (or handler method) whose path carries an {@code orgId}
 * (or {@code organizationId}) template variable and requires the caller to be a member of that org.
 * Enforced by {@link OrgAccessAspect} before the handler runs.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireOrgAccess {
}
