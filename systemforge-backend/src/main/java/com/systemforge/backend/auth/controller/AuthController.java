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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Registration, login, OTP, and token management")
public class AuthController {

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
                .body(ApiResponse.success("Registration successful", response));
    }

    // ================= LOGIN =================

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Login", description = "Authenticate with email and password")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        return ResponseEntity.ok(
                ApiResponse.success("Login successful", authService.login(request))
        );
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

        return ResponseEntity.ok(
                ApiResponse.success("Authentication successful",
                        authService.verifyOtp(request))
        );
    }

    // ================= REFRESH =================

    @PostMapping("/refresh")
    @SecurityRequirements
    @Operation(summary = "Refresh token")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @RequestHeader("X-Refresh-Token") String refreshToken) {

        if (!StringUtils.hasText(refreshToken)) {
            throw new IllegalArgumentException("Refresh token is required");
        }

        return ResponseEntity.ok(
                ApiResponse.success("Token refreshed",
                        authService.refreshToken(refreshToken))
        );
    }

    // ================= LOGOUT =================

    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Revoke all refresh tokens")
    public ResponseEntity<ApiResponse<Void>> logout() {

        String userId = securityService.getAuthenticatedUserId().toString();

        authService.logout(userId);

        return ResponseEntity.ok(
                ApiResponse.success("Logged out successfully")
        );
    }
}