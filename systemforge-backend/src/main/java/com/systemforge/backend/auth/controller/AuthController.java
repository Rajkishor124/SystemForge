package com.systemforge.backend.auth.controller;

import com.systemforge.backend.auth.dto.request.LoginRequest;
import com.systemforge.backend.auth.dto.request.RegisterRequest;
import com.systemforge.backend.auth.dto.request.SendOtpRequest;
import com.systemforge.backend.auth.dto.request.VerifyOtpRequest;
import com.systemforge.backend.auth.dto.response.AuthResponse;
import com.systemforge.backend.auth.service.AuthService;
import com.systemforge.backend.auth.service.SecurityService;
import com.systemforge.backend.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.UUID;

/**
 * Authentication REST controller.
 *
 * <p>Security model:
 * <ul>
 *   <li>Access token → HttpOnly, Secure, SameSite=Strict cookie on path /api/</li>
 *   <li>Refresh token → HttpOnly, Secure, SameSite=Strict cookie on path /api/v1/auth/refresh</li>
 *   <li>Authorization header fallback maintained for API clients (Swagger, Postman, mobile)</li>
 *   <li>Response body still carries userId/role for frontend state hydration</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Registration, login, OTP, and token management")
public class AuthController {

    private static final String ACCESS_COOKIE  = "sf_access_token";
    private static final String REFRESH_COOKIE = "sf_refresh_token";
    private static final Duration ACCESS_MAX_AGE  = Duration.ofMinutes(15);
    private static final Duration REFRESH_MAX_AGE = Duration.ofDays(7);

    private final AuthService authService;
    private final SecurityService securityService;

    // ================= REGISTER =================

    @PostMapping("/register")
    @SecurityRequirements
    @Operation(summary = "Register", description = "Creates a new DEVELOPER account")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {

        AuthResponse response = authService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .headers(buildAuthCookieHeaders(response))
                .body(ApiResponse.success("Registration successful", response.withoutTokens()));
    }

    // ================= LOGIN =================

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Login", description = "Authenticate with email and password")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        AuthResponse response = authService.login(request);

        return ResponseEntity.ok()
                .headers(buildAuthCookieHeaders(response))
                .body(ApiResponse.success("Login successful", response.withoutTokens()));
    }

    // ================= OTP =================

    @PostMapping("/otp/send")
    @SecurityRequirements
    @Operation(summary = "Send OTP")
    public ResponseEntity<ApiResponse<Void>> sendOtp(
            @Valid @RequestBody SendOtpRequest request) {

        authService.sendOtp(request);

        return ResponseEntity.ok(
                ApiResponse.success("OTP sent successfully")
        );
    }

    @PostMapping("/otp/verify")
    @SecurityRequirements
    @Operation(summary = "Verify OTP")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request) {

        AuthResponse response = authService.verifyOtp(request);

        return ResponseEntity.ok()
                .headers(buildAuthCookieHeaders(response))
                .body(ApiResponse.success("Authentication successful", response.withoutTokens()));
    }

    // ================= REFRESH =================

    @PostMapping("/refresh")
    @SecurityRequirements
    @Operation(summary = "Refresh token", description = "Reads refresh token from HttpOnly cookie or X-Refresh-Token header")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            HttpServletRequest httpRequest,
            @RequestHeader(value = "X-Refresh-Token", required = false) String headerRefreshToken) {

        // Cookie-first, header fallback
        String refreshToken = extractRefreshTokenFromCookie(httpRequest);
        if (!StringUtils.hasText(refreshToken)) {
            refreshToken = headerRefreshToken;
        }

        if (!StringUtils.hasText(refreshToken)) {
            throw new IllegalArgumentException("Refresh token is required (cookie or X-Refresh-Token header)");
        }

        AuthResponse response = authService.refreshToken(refreshToken);

        return ResponseEntity.ok()
                .headers(buildAuthCookieHeaders(response))
                .body(ApiResponse.success("Token refreshed", response.withoutTokens()));
    }

    // ================= LOGOUT =================

    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Revoke all refresh tokens and clear auth cookies")
    public ResponseEntity<ApiResponse<Void>> logout() {

        String userId = securityService.getAuthenticatedUserId().toString();
        authService.logout(userId);

        return ResponseEntity.ok()
                .headers(buildClearCookieHeaders())
                .body(ApiResponse.success("Logged out successfully"));
    }

    // ================= ME (Session Check) =================

    @GetMapping("/me")
    @Operation(summary = "Get current user", description = "Returns authenticated user info from the cookie session / JWT")
    public ResponseEntity<ApiResponse<AuthResponse>> me() {
        UUID userId = securityService.getAuthenticatedUserId();
        // SecurityContext is already populated by JwtAuthenticationFilter
        // Extract role from the existing authentication
        String role = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("DEVELOPER");

        AuthResponse response = AuthResponse.builder()
                .userId(userId.toString())
                .role(role)
                .build();

        return ResponseEntity.ok(ApiResponse.success("User info retrieved", response));
    }

    // ================= COOKIE HELPERS =================

    private HttpHeaders buildAuthCookieHeaders(AuthResponse auth) {
        HttpHeaders headers = new HttpHeaders();

        // Access token cookie — available to all /api/ paths
        ResponseCookie accessCookie = ResponseCookie.from(ACCESS_COOKIE, auth.getAccessToken())
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/api/")
                .maxAge(ACCESS_MAX_AGE)
                .build();

        // Refresh token cookie — scoped to /api/v1/auth/refresh only
        ResponseCookie refreshCookie = ResponseCookie.from(REFRESH_COOKIE, auth.getRefreshToken())
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/api/v1/auth/refresh")
                .maxAge(REFRESH_MAX_AGE)
                .build();

        headers.add(HttpHeaders.SET_COOKIE, accessCookie.toString());
        headers.add(HttpHeaders.SET_COOKIE, refreshCookie.toString());
        return headers;
    }

    private HttpHeaders buildClearCookieHeaders() {
        HttpHeaders headers = new HttpHeaders();

        ResponseCookie clearAccess = ResponseCookie.from(ACCESS_COOKIE, "")
                .httpOnly(true).secure(true).sameSite("Strict")
                .path("/api/").maxAge(0).build();

        ResponseCookie clearRefresh = ResponseCookie.from(REFRESH_COOKIE, "")
                .httpOnly(true).secure(true).sameSite("Strict")
                .path("/api/v1/auth/refresh").maxAge(0).build();

        headers.add(HttpHeaders.SET_COOKIE, clearAccess.toString());
        headers.add(HttpHeaders.SET_COOKIE, clearRefresh.toString());
        return headers;
    }

    private String extractRefreshTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        for (Cookie cookie : request.getCookies()) {
            if (REFRESH_COOKIE.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}