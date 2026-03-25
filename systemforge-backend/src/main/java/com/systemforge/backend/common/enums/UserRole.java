package com.systemforge.backend.common.enums;

/**
 * User roles within SystemForge.
 *
 * <p>Role design rationale:
 * <ul>
 *   <li>{@code ADMIN} — Platform operators; full access including template management,
 *       user oversight, and system monitoring</li>
 *   <li>{@code DEVELOPER} — End-user persona; can design, configure, and generate
 *       system architectures within their own workspace</li>
 * </ul>
 *
 * <p>When Spring Security is integrated, these map to authorities prefixed with {@code ROLE_},
 * e.g., {@code ROLE_ADMIN}, enabling standard {@code @PreAuthorize("hasRole('ADMIN')")} usage.
 */
public enum UserRole {

    /**
     * Platform administrator.
     * Has full access to templates, user management, and system monitoring.
     */
    ADMIN,

    /**
     * Developer / end-user.
     * Can design architectures and generate blueprints within their own scope.
     */
    DEVELOPER
}