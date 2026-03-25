package com.systemforge.backend.user.dto.response;

import com.systemforge.backend.common.enums.UserRole;
import com.systemforge.backend.user.enums.AccountStatus;
import com.systemforge.backend.user.enums.AuthProvider;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * External representation of a User.
 *
 * <p>Entity is NEVER exposed directly.
 * This DTO ensures safe and controlled API responses.
 */
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
@AllArgsConstructor
@Schema(description = "User profile data returned in API responses")
public class UserResponse {

    @Schema(description = "User's unique identifier")
    private final UUID id;

    @Schema(description = "User's full name")
    private final String name;

    @Schema(description = "User's email address")
    private final String email;

    @Schema(description = "User role")
    private final UserRole role;

    @Schema(description = "Account status")
    private final AccountStatus accountStatus;

    @Schema(description = "Authentication provider used")
    private final AuthProvider authProvider;

    @Schema(description = "Whether email is verified")
    private final boolean emailVerified;

    @Schema(description = "Last login timestamp")
    private final Instant lastLoginAt;

    @Schema(description = "Account creation timestamp")
    private final Instant createdAt;

    @Schema(description = "Last update timestamp")
    private final Instant updatedAt;
}