package com.systemforge.backend.admin.service;

import com.systemforge.backend.admin.dto.PlatformStatsDto;
import com.systemforge.backend.user.dto.response.UserResponse;

import java.util.UUID;

/**
 * Admin service contract for platform management operations.
 *
 * <p>All methods here require ADMIN role (enforced via @PreAuthorize in Phase 2).
 * This service orchestrates across multiple domain modules — which is acceptable
 * at the Admin layer since it's explicitly an orchestration concern, not a domain concern.
 *
 * <p>Admins can:
 * <ul>
 *   <li>View platform-wide statistics</li>
 *   <li>Manage users (activate/deactivate, role changes)</li>
 *   <li>Manage system definitions (add, enable, disable)</li>
 *   <li>Manage templates (seed, update, disable)</li>
 * </ul>
 */
public interface AdminService {

    /** Returns aggregated platform statistics: user count, config count, generation count. */
    PlatformStatsDto getPlatformStats();

    /** Activates a deactivated user account. */
    UserResponse activateUser(UUID userId);

    /** Deactivates a user account without soft-deleting — preserves data, blocks login. */
    UserResponse deactivateUser(UUID userId);

    /** Changes a user's role (e.g., DEVELOPER → ADMIN). Requires ADMIN authority. */
    UserResponse changeUserRole(UUID userId, String newRole);

    /** Retrieves a paginated list of all users. */
    org.springframework.data.domain.Page<UserResponse> getAllUsers(org.springframework.data.domain.Pageable pageable);
}