package com.systemforge.backend.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Structured HTTP request/response logging filter for operational observability.
 *
 * <p>Produces one log line per request with all the data ops teams need:
 * <pre>
 * [HTTP] method=POST uri=/api/v1/systems/configs/123/generate status=202 latencyMs=45 userId=abc ip=1.2.3.4 correlationId=xyz
 * </pre>
 *
 * <p>Design decisions:
 * <ul>
 *   <li>Runs at {@code @Order(2)} — after CorrelationIdFilter (Order 1) so MDC is populated</li>
 *   <li>Skips actuator and static asset paths to reduce noise</li>
 *   <li>Extracts userId from SecurityContext (populated by JwtAuthenticationFilter)</li>
 *   <li>Uses {@code System.nanoTime()} for accurate latency measurement</li>
 *   <li>Logs at INFO level for production log aggregation</li>
 * </ul>
 */
@Component
@Order(2)
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        long startNanos = System.nanoTime();

        try {
            filterChain.doFilter(request, response);
        } finally {
            long latencyMs = (System.nanoTime() - startNanos) / 1_000_000;

            String method = request.getMethod();
            String uri = request.getRequestURI();
            int status = response.getStatus();
            String userId = resolveUserId();
            String ip = resolveClientIp(request);

            log.info("[HTTP] method={} uri={} status={} latencyMs={} userId={} ip={}",
                    method, uri, status, latencyMs, userId, ip);
        }
    }

    /**
     * Skip logging for noisy paths that would drown out meaningful request logs.
     */
    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator")
                || path.startsWith("/swagger")
                || path.startsWith("/api-docs")
                || path.endsWith(".css")
                || path.endsWith(".js")
                || path.endsWith(".ico")
                || path.endsWith(".png")
                || path.endsWith(".jpg");
    }

    private String resolveUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof String userId) {
            return userId;
        }
        return "anonymous";
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
