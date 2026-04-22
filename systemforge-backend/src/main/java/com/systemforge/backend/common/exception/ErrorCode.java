package com.systemforge.backend.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Machine-readable error codes for all SystemForge API errors.
 *
 * <p>Convention: {@code MODULE_NNN} where:
 * <ul>
 *   <li>{@code AUTH_} — authentication & authorization</li>
 *   <li>{@code RATE_} — rate limiting</li>
 *   <li>{@code SYS_} — system config & generation</li>
 *   <li>{@code AI_} — AI/LLM provider errors</li>
 *   <li>{@code VAL_} — input validation</li>
 *   <li>{@code GEN_} — general/infrastructure</li>
 * </ul>
 *
 * <p>Clients should switch on these codes for programmatic error handling
 * instead of string-matching on messages. Messages are for humans, codes are for machines.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // ─── Auth ─────────────────────────────────────────────────────────────────
    AUTH_001("Invalid credentials"),
    AUTH_002("Token expired"),
    AUTH_003("Token invalid or malformed"),
    AUTH_004("Account locked or disabled"),
    AUTH_005("Email not verified"),
    AUTH_006("Refresh token expired or revoked"),
    AUTH_007("OTP expired or invalid"),
    AUTH_008("Too many OTP attempts"),

    // ─── Rate Limiting ────────────────────────────────────────────────────────
    RATE_001("Rate limit exceeded — try again later"),

    // ─── System / Generation ──────────────────────────────────────────────────
    SYS_001("System config not found"),
    SYS_002("Architecture already generated for this config"),
    SYS_003("Generation already in progress"),
    SYS_004("Generation failed"),
    SYS_005("Invalid system configuration"),

    // ─── AI / Resilience ──────────────────────────────────────────────────────
    AI_001("AI service unavailable — circuit breaker open"),
    AI_002("AI service timeout — request took too long"),
    AI_003("AI fallback response — using rule-based engine"),

    // ─── Validation ───────────────────────────────────────────────────────────
    VAL_001("Validation failed — check field errors"),
    VAL_002("Invalid input format"),
    VAL_003("Request body too large"),

    // ─── General ──────────────────────────────────────────────────────────────
    GEN_001("Resource not found"),
    GEN_002("Duplicate resource"),
    GEN_003("Access denied — insufficient permissions"),
    GEN_004("Internal server error"),
    GEN_005("Service temporarily unavailable");

    private final String defaultMessage;

    /**
     * Returns the full code string (e.g., "AUTH_001").
     */
    public String code() {
        return this.name();
    }
}
