package com.systemforge.backend.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Servlet filter that ensures every request carries a Correlation ID.
 *
 * <p>Correlation ID lifecycle:
 * <ol>
 *   <li>If the incoming request carries {@code X-Correlation-ID}, use it (enables
 *       end-to-end tracing from gateway/client)</li>
 *   <li>If not present, generate a new UUID v4</li>
 *   <li>Store in SLF4J MDC so every log line in the request thread includes it</li>
 *   <li>Echo back in the response header for client-side correlation</li>
 *   <li>Clear MDC after response to prevent thread-local leakage in thread pools</li>
 * </ol>
 *
 * <p>Ordered at highest precedence ({@code @Order(1)}) so it runs before security filters
 * and all logs within the security chain are already correlated.
 */
@Component
@Order(1)
@Slf4j
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String MDC_CORRELATION_KEY = "correlationId";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String correlationId = resolveCorrelationId(request);

        try {
            MDC.put(MDC_CORRELATION_KEY, correlationId);
            response.setHeader(CORRELATION_ID_HEADER, correlationId);

            log.debug("Request started: method={} uri={} correlationId={}",
                    request.getMethod(), request.getRequestURI(), correlationId);

            filterChain.doFilter(request, response);

            log.debug("Request completed: status={} correlationId={}",
                    response.getStatus(), correlationId);

        } finally {
            // Critical: always clear MDC to prevent correlation ID leakage
            // across requests in pooled/async servlet containers
            MDC.remove(MDC_CORRELATION_KEY);
        }
    }

    /**
     * Reads the correlation ID from the request header, or generates a fresh UUID.
     *
     * <p>Accepts incoming IDs from upstream gateways/clients to support
     * end-to-end distributed tracing without requiring a dedicated tracing framework.
     */
    private String resolveCorrelationId(HttpServletRequest request) {
        String incoming = request.getHeader(CORRELATION_ID_HEADER);
        if (StringUtils.hasText(incoming)) {
            return incoming;
        }
        return UUID.randomUUID().toString();
    }
}