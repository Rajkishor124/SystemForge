package com.systemforge.backend.common.audit;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Structured security audit logger for SIEM integration.
 *
 * <p>Writes structured log lines to a dedicated {@code AUDIT} logger,
 * separate from the application logger. This makes it easy to route
 * audit events to a dedicated log file, ELK stack, or Splunk.
 *
 * <p>All events include:
 * <ul>
 *   <li>Event type (e.g., LOGIN_SUCCESS, LOGIN_FAILED)</li>
 *   <li>Timestamp (ISO-8601 UTC)</li>
 *   <li>Correlation ID (from MDC)</li>
 *   <li>Contextual data (email, IP, userId, etc.)</li>
 * </ul>
 *
 * <p>Design: Uses SLF4J structured arguments instead of raw JSON string
 * concatenation. This is compatible with Logback's JSON encoder and
 * any structured logging framework.
 */
@Component
@Slf4j
public class SecurityAuditLogger {

    private static final Logger AUDIT = LoggerFactory.getLogger("AUDIT");

    // ─── Authentication Events ────────────────────────────────────────────────

    public void logLoginSuccess(String email, String ip, String userAgent) {
        AUDIT.info("[AUDIT] event=LOGIN_SUCCESS email={} ip={} userAgent={} correlationId={} timestamp={}",
                email, ip, sanitize(userAgent), correlationId(), Instant.now());
    }

    public void logLoginFailed(String email, String ip, String reason) {
        AUDIT.warn("[AUDIT] event=LOGIN_FAILED email={} ip={} reason={} correlationId={} timestamp={}",
                email, ip, reason, correlationId(), Instant.now());
    }

    public void logTokenRefresh(UUID userId, String ip) {
        AUDIT.info("[AUDIT] event=TOKEN_REFRESH userId={} ip={} correlationId={} timestamp={}",
                userId, ip, correlationId(), Instant.now());
    }

    public void logLogout(UUID userId, String ip) {
        AUDIT.info("[AUDIT] event=LOGOUT userId={} ip={} correlationId={} timestamp={}",
                userId, ip, correlationId(), Instant.now());
    }

    public void logOtpRequested(String email, String ip) {
        AUDIT.info("[AUDIT] event=OTP_REQUESTED email={} ip={} correlationId={} timestamp={}",
                email, ip, correlationId(), Instant.now());
    }

    // ─── Generation Events ────────────────────────────────────────────────────

    public void logGenerationStarted(UUID userId, UUID configId, UUID jobId) {
        AUDIT.info("[AUDIT] event=GENERATION_STARTED userId={} configId={} jobId={} correlationId={} timestamp={}",
                userId, configId, jobId, correlationId(), Instant.now());
    }

    public void logGenerationCompleted(UUID userId, UUID jobId, long durationMs) {
        AUDIT.info("[AUDIT] event=GENERATION_COMPLETED userId={} jobId={} durationMs={} correlationId={} timestamp={}",
                userId, jobId, durationMs, correlationId(), Instant.now());
    }

    public void logGenerationFailed(UUID userId, UUID jobId, String reason) {
        AUDIT.warn("[AUDIT] event=GENERATION_FAILED userId={} jobId={} reason={} correlationId={} timestamp={}",
                userId, jobId, reason, correlationId(), Instant.now());
    }

    // ─── Rate Limiting Events ─────────────────────────────────────────────────

    public void logRateLimitHit(String key, String endpoint, String ip) {
        AUDIT.warn("[AUDIT] event=RATE_LIMIT_HIT key={} endpoint={} ip={} correlationId={} timestamp={}",
                key, endpoint, ip, correlationId(), Instant.now());
    }

    // ─── Circuit Breaker Events ───────────────────────────────────────────────

    public void logCircuitBreakerStateChange(String fromState, String toState) {
        AUDIT.warn("[AUDIT] event=CIRCUIT_BREAKER_STATE_CHANGE from={} to={} correlationId={} timestamp={}",
                fromState, toState, correlationId(), Instant.now());
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private String correlationId() {
        String id = MDC.get("correlationId");
        return id != null ? id : "N/A";
    }

    /**
     * Sanitize user agent strings to prevent log injection.
     */
    private String sanitize(String input) {
        if (input == null) return "N/A";
        return input.replaceAll("[\\r\\n]", "")
                    .substring(0, Math.min(input.length(), 200));
    }
}
