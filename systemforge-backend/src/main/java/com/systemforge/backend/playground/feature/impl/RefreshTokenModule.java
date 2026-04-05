package com.systemforge.backend.playground.feature.impl;

import com.systemforge.backend.playground.engine.TemplateCompositionContext;
import com.systemforge.backend.playground.enums.FeatureToggle;
import com.systemforge.backend.playground.enums.ServiceType;
import com.systemforge.backend.playground.enums.ServiceVariant;
import com.systemforge.backend.playground.feature.FeatureModule;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Feature Module: Refresh Token support for JWT authentication.
 *
 * <p>Injects refresh token generation, storage, and rotation logic
 * into the appropriate template placeholders.
 */
@Component
public class RefreshTokenModule implements FeatureModule {

    @Override
    public FeatureToggle getSupportedToggle() {
        return FeatureToggle.REFRESH_TOKEN;
    }

    @Override
    public boolean isCompatibleWith(ServiceType type, ServiceVariant variant) {
        return type == ServiceType.AUTH && variant == ServiceVariant.JWT;
    }

    @Override
    public List<FeatureToggle> getDependencies() {
        return List.of(); // No dependencies — standalone feature
    }

    @Override
    public void apply(TemplateCompositionContext context) {

        // ─── Controller: Add refresh endpoint ──────────────────────
        context.setPlaceholder("REFRESH_TOKEN_ENDPOINT", """
                @PostMapping("/refresh")
                public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
                        @RequestHeader("X-Refresh-Token") String refreshToken) {
                    AuthResponse response = authService.refreshAccessToken(refreshToken);
                    return ResponseEntity.ok(ApiResponse.success("Token refreshed", response));
                }
                """);

        // ─── Service: Add refresh token generation ─────────────────
        context.setPlaceholder("REFRESH_TOKEN_SERVICE", """
                    String refreshToken = tokenProvider.generateRefreshToken(user);
                    // Store refresh token in DB for rotation & revocation
                    tokenProvider.storeRefreshToken(user.getId(), refreshToken);
                """);

        // ─── Config: Add refresh token TTL ─────────────────────────
        context.setPlaceholder("REFRESH_TOKEN_CONFIG", """
                @Value("${jwt.refresh-expiration-ms:604800000}")
                private long refreshTokenExpirationMs; // 7 days
                """);

        // ─── Architecture ──────────────────────────────────────────
        context.addArchitectureStep(
                "6. Refresh token is returned alongside access token (stored in HttpOnly cookie)");
        context.addArchitectureStep(
                "7. Client calls `/api/auth/refresh` with expired access token to get a new pair");
        context.addArchitectureStep(
                "8. Old refresh token is rotated on each use (prevents replay attacks)");

        context.appendToPlaceholder("ARCHITECTURE_EXTENSIONS", """
                
                **Refresh Token Flow:**
                - Refresh tokens are long-lived (7 days) and stored server-side
                - Token rotation on every refresh (old token invalidated)
                - Revocation support via DB lookup
                """);

        // ─── Components & Stack ────────────────────────────────────
        context.addComponent("RefreshTokenRepository");
        context.addComponent("TokenRotationService");
        context.addTechStack("Redis (optional, for token blacklist)");
    }
}
