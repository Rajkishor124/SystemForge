package com.systemforge.backend.auth.util;

import com.systemforge.backend.common.exception.BusinessException;
import lombok.experimental.UtilityClass;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

/**
 * Utility for extracting authenticated user details from Spring Security context.
 *
 * <p>Design Principles:
 * - Never trust client-provided userId
 * - Always extract identity from JWT (SecurityContext)
 * - Fail fast if authentication is invalid
 *
 * <p>Used across service layer for secure identity access.
 */
@UtilityClass
public class SecurityPrincipalUtil {

    /**
     * Returns the authenticated user's UUID from the security context.
     *
     * @return userId as UUID
     * @throws BusinessException if authentication is missing or invalid
     */
    public static UUID getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (!isAuthenticated(authentication)) {
            throw new BusinessException(
                    "AUTH_REQUIRED",
                    "Authentication required",
                    HttpStatus.UNAUTHORIZED
            );
        }

        Object principal = authentication.getPrincipal();

        try {
            if (principal instanceof UUID uuid) {
                return uuid;
            }

            if (principal instanceof String str) {
                return UUID.fromString(str);
            }

            throw new IllegalArgumentException("Unsupported principal type: " + principal.getClass());

        } catch (Exception e) {
            throw new BusinessException(
                    "AUTH_INVALID_PRINCIPAL",
                    "Invalid authentication state",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    /**
     * Returns the authenticated user's role (without ROLE_ prefix).
     *
     * Example: "ADMIN", "DEVELOPER"
     */
    public static String getAuthenticatedRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (!isAuthenticated(authentication) || authentication.getAuthorities() == null) {
            return null;
        }

        return authentication.getAuthorities().stream()
                .findFirst()
                .map(authority -> authority.getAuthority().replace("ROLE_", ""))
                .orElse(null);
    }

    /**
     * Checks if current user has ADMIN role.
     */
    public static boolean isAdmin() {
        return "ADMIN".equals(getAuthenticatedRole());
    }

    /**
     * Checks if current user has DEVELOPER role.
     */
    public static boolean isDeveloper() {
        return "DEVELOPER".equals(getAuthenticatedRole());
    }

    /**
     * Checks whether a valid authenticated user exists.
     */
    public static boolean isAuthenticated() {
        return isAuthenticated(SecurityContextHolder.getContext().getAuthentication());
    }

    /**
     * Internal helper for authentication validation.
     */
    private static boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal());
    }
}