package com.systemforge.backend.common.enums;

/**
 * Authentication strategy types supported by SystemForge's Auth module.
 *
 * <p>Used by the template engine to generate appropriate auth architecture
 * based on the developer's chosen strategy.
 */
public enum AuthType {

    /**
     * One-Time Password authentication.
     * Common for mobile-first apps (ride-hailing, delivery, fintech).
     * Typically via SMS or email OTP.
     */
    OTP,

    /**
     * Traditional email and password authentication.
     * Standard for SaaS products, admin panels, and B2B platforms.
     * Must be paired with secure password hashing (BCrypt/Argon2).
     */
    EMAIL_PASSWORD,

    /**
     * Social / OAuth2 login.
     * Delegates authentication to a trusted identity provider (Google, GitHub, Apple).
     * Reduces friction in consumer apps. Future implementation.
     */
    SOCIAL,

    /**
     * Combined OTP + Password (step-up authentication).
     * Used in high-security contexts like fintech and healthcare.
     */
    OTP_AND_PASSWORD,

    /**
     * API Key-based authentication.
     * For machine-to-machine (M2M) communication and developer APIs.
     */
    API_KEY
}