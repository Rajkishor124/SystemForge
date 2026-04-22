package com.systemforge.backend.common.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.systemforge.backend.common.config.RateLimitConfig;
import com.systemforge.backend.common.dto.ApiResponse;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Rate limiting interceptor using Bucket4j's token bucket algorithm.
 *
 * <p>Bucket keying strategy:
 * <ul>
 *   <li>Public endpoints (OTP, login) → keyed by client IP address</li>
 *   <li>Authenticated endpoints (generate, architect/chat) → keyed by userId</li>
 * </ul>
 *
 * <p>On rejection, returns 429 Too Many Requests with a Retry-After header
 * indicating how many seconds the client should wait.
 *
 * <p>Buckets are stored in a ConcurrentHashMap (in-memory). For multi-instance
 * deployments, this should be migrated to Redis-backed Bucket4j (Phase 2).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitingInterceptor implements HandlerInterceptor {

    private final RateLimitConfig rateLimitConfig;
    private final ObjectMapper objectMapper;

    /**
     * In-memory bucket store. Thread-safe.
     * Key format: "{category}:{identifier}" e.g. "otp:192.168.1.1" or "generate:user-uuid"
     */
    private final ConcurrentMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler) throws IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        // Only rate-limit mutating operations (POST, PUT, PATCH)
        if ("GET".equalsIgnoreCase(method) || "OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }

        // Determine which bucket category applies
        RateLimitCategory category = resolveCategory(path);
        if (category == null) {
            return true; // No rate limit for this path
        }

        // Resolve the bucket key
        String key = category.name().toLowerCase() + ":" + resolveKey(category, request);
        RateLimitConfig.BucketSpec spec = resolveSpec(category);

        // Get or create the bucket
        Bucket bucket = buckets.computeIfAbsent(key, k -> createBucket(spec));

        // Try to consume a token
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            // Informational headers
            response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            return true;
        }

        // Rejected — 429 Too Many Requests
        long waitSeconds = probe.getNanosToWaitForRefill() / 1_000_000_000;
        log.warn("Rate limit exceeded: category={}, key={}, retryAfterSec={}", category, key, waitSeconds);

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.addHeader("Retry-After", String.valueOf(waitSeconds));
        response.addHeader("X-Rate-Limit-Remaining", "0");

        ApiResponse<Void> errorResponse = ApiResponse.error(
                "Rate limit exceeded. Please try again after " + waitSeconds + " seconds."
        );
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
        return false;
    }

    // ─── Category Resolution ──────────────────────────────────────────────

    private enum RateLimitCategory {
        OTP, LOGIN, GENERATE, ARCHITECT_CHAT
    }

    private RateLimitCategory resolveCategory(String path) {
        if (path.contains("/auth/otp/")) return RateLimitCategory.OTP;
        if (path.contains("/auth/login")) return RateLimitCategory.LOGIN;
        if (path.contains("/generate")) return RateLimitCategory.GENERATE;
        if (path.contains("/architect/chat")) return RateLimitCategory.ARCHITECT_CHAT;
        return null;
    }

    // ─── Key Resolution ───────────────────────────────────────────────────

    private String resolveKey(RateLimitCategory category, HttpServletRequest request) {
        return switch (category) {
            case OTP, LOGIN -> resolveClientIp(request);
            case GENERATE, ARCHITECT_CHAT -> resolveUserId(request);
        };
    }

    private String resolveClientIp(HttpServletRequest request) {
        // Support reverse proxy headers
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            return xff.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isEmpty()) {
            return realIp;
        }
        return request.getRemoteAddr();
    }

    private String resolveUserId(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof String userId) {
            return userId;
        }
        // Fallback to IP if not authenticated (shouldn't happen for protected endpoints)
        return resolveClientIp(request);
    }

    // ─── Bucket Factory ───────────────────────────────────────────────────

    private RateLimitConfig.BucketSpec resolveSpec(RateLimitCategory category) {
        return switch (category) {
            case OTP -> rateLimitConfig.getOtp();
            case LOGIN -> rateLimitConfig.getLogin();
            case GENERATE -> rateLimitConfig.getGenerate();
            case ARCHITECT_CHAT -> rateLimitConfig.getArchitectChat();
        };
    }

    private Bucket createBucket(RateLimitConfig.BucketSpec spec) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(spec.getCapacity())
                .refillGreedy(spec.getCapacity(), Duration.ofMinutes(spec.getRefillMinutes()))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }
}
