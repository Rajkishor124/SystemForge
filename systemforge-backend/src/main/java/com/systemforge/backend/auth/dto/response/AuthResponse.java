package com.systemforge.backend.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * Token pair returned after authentication.
 */
@Getter
@Builder
@Schema(description = "JWT token pair")
public class AuthResponse {

    @Schema(description = "Access token")
    private final String accessToken;

    @Schema(description = "Refresh token")
    private final String refreshToken;

    @Builder.Default
    @Schema(description = "Token type", example = "Bearer")
    private String tokenType = "Bearer";

    @Schema(description = "Access token expiry")
    private final Instant accessTokenExpiresAt;

    @Schema(description = "User ID")
    private final String userId;

    @Schema(description = "User role")
    private final String role;
}