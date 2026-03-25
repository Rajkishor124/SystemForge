package com.systemforge.backend.user.enums;

/**
 * Represents the authentication method used by a user.
 *
 * This allows the system to support multiple login mechanisms
 * in a clean and extensible way.
 */
public enum AuthProvider {

    /**
     * Traditional email + password authentication.
     */
    LOCAL,

    /**
     * Authentication using email-based OTP.
     */
    EMAIL_OTP,

    /**
     * OAuth login via Google.
     * (Future implementation)
     */
    GOOGLE
}