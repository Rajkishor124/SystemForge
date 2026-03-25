package com.systemforge.backend.auth.service;

import java.util.UUID;

/**
 * Service for accessing current security context information.
 *
 * <p>This interface abstracts Spring Security's static context, making
 * domain services easier to unit test without complex mocking of static methods.
 */
public interface SecurityService {

    /**
     * Get the ID of the currently authenticated user.
     *
     * @return User UUID
     */
    UUID getAuthenticatedUserId();

    /**
     * Get the role of the currently authenticated user.
     *
     * @return User role (e.g., ADMIN, DEVELOPER)
     */
    String getAuthenticatedRole();

    /**
     * Check if the current user has the ADMIN role.
     */
    boolean isAdmin();

    /**
     * Check if the current user has the DEVELOPER role.
     */
    boolean isDeveloper();

    /**
     * Check if the request is authenticated.
     */
    boolean isAuthenticated();
}
