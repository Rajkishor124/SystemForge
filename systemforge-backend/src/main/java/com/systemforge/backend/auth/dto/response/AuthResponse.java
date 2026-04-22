package com.systemforge.backend.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * Authentication response.
 *
 * <p>After the HttpOnly cookie migration, raw tokens are only sent via
 * Set-Cookie headers — never in the JSON body for browser clients.
 * The {@link #withoutTokens()} method returns a copy safe for the response body.
 *
 * <p>API clients (Swagger, Postman) can still use the Authorization header
 * fallback, but the preferred browser flow is cookie-based.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Authentication result")
public class AuthResponse {

    @Schema(description = "Access token (only in Set-Cookie header for browsers)")
    private final String accessToken;

    @Schema(description = "Refresh token (only in Set-Cookie header for browsers)")
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

    /**
     * Returns a copy with tokens stripped — safe for the JSON response body.
     * Tokens are delivered via HttpOnly cookies instead.
     */
    public AuthResponse withoutTokens() {
        return AuthResponse.builder()
                .userId(this.userId)
                .role(this.role)
                .tokenType(this.tokenType)
                .accessTokenExpiresAt(this.accessTokenExpiresAt)
                // accessToken and refreshToken intentionally omitted
                .build();
    }
}