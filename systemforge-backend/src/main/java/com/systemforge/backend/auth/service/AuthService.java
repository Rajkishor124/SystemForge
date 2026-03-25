package com.systemforge.backend.auth.service;

import com.systemforge.backend.auth.dto.request.LoginRequest;
import com.systemforge.backend.auth.dto.request.RegisterRequest;
import com.systemforge.backend.auth.dto.request.SendOtpRequest;
import com.systemforge.backend.auth.dto.request.VerifyOtpRequest;
import com.systemforge.backend.auth.dto.response.AuthResponse;

/**
 * Authentication service contract.
 *
 * <p>Handles all authentication-related flows:
 *
 * <ol>
 *   <li><b>Registration</b> — create new user account</li>
 *   <li><b>Email/Password Login</b></li>
 *   <li><b>OTP Authentication</b></li>
 *   <li><b>Token Management</b> — refresh & logout</li>
 * </ol>
 *
 * <p>All implementations must:
 * - Enforce security best practices
 * - Never expose sensitive information
 * - Handle authentication failures uniformly
 */
public interface AuthService {

    // ================= REGISTRATION =================

    /**
     * Registers a new user using email and password.
     *
     * @param request registration request
     * @return authentication response with token pair
     */
    AuthResponse register(RegisterRequest request);

    // ================= LOGIN =================

    /**
     * Authenticates user using email and password.
     */
    AuthResponse login(LoginRequest request);

    // ================= OTP FLOW =================

    /**
     * Sends OTP to user's email.
     */
    void sendOtp(SendOtpRequest request);

    /**
     * Verifies OTP and authenticates user.
     */
    AuthResponse verifyOtp(VerifyOtpRequest request);

    // ================= TOKEN MANAGEMENT =================

    /**
     * Issues a new access + refresh token pair using a valid refresh token.
     */
    AuthResponse refreshToken(String rawRefreshToken);

    /**
     * Logs out user by revoking all refresh tokens.
     */
    void logout(String userId);
}