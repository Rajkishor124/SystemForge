package com.systemforge.backend.auth.filter;

import com.systemforge.backend.auth.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT authentication filter — extracts and validates access tokens.
 *
 * <p>Token extraction priority:
 * <ol>
 *   <li>HttpOnly cookie {@code sf_access_token} (browser clients)</li>
 *   <li>Authorization header {@code Bearer <token>} (API clients, Swagger, Postman)</li>
 * </ol>
 *
 * <p>This dual-source approach ensures backward compatibility for non-browser
 * API consumers while securing browser sessions with HttpOnly cookies.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String ACCESS_COOKIE       = "sf_access_token";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX        = "Bearer ";
    private static final String ROLE_PREFIX          = "ROLE_";

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        try {

            // 1. Skip if already authenticated
            if (SecurityContextHolder.getContext().getAuthentication() != null &&
                    SecurityContextHolder.getContext().getAuthentication().isAuthenticated()) {
                filterChain.doFilter(request, response);
                return;
            }

            // 2. Extract token (cookie-first, header fallback)
            String token = extractToken(request);

            if (token == null) {
                filterChain.doFilter(request, response);
                return;
            }

            // 3. Parse + validate (single operation)
            Claims claims = jwtService.parseToken(token);

            // 4. Check token type
            if (!jwtService.isAccessToken(claims)) {
                log.debug("JWT is not an access token");
                SecurityContextHolder.clearContext();
                filterChain.doFilter(request, response);
                return;
            }

            // 5. Extract data
            String userId = jwtService.extractUserId(claims);
            String role   = jwtService.extractRole(claims);

            // 6. Build authorities
            List<SimpleGrantedAuthority> authorities = List.of(
                    new SimpleGrantedAuthority(ROLE_PREFIX + role)
            );

            // 7. Create authentication
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userId, null, authorities);

            authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.debug("JWT authenticated userId={} role={}", userId, role);

        } catch (JwtException e) {
            log.warn("JWT processing failed: {}", e.getClass().getSimpleName());
            SecurityContextHolder.clearContext();
        } catch (Exception e) {
            log.error("Unexpected authentication error", e);
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Ensures this filter runs during async dispatch (required for SSE).
     * By default, OncePerRequestFilter skips async dispatches, causing SseEmitters
     * to throw Access Denied on the async thread and break with ERR_INCOMPLETE_CHUNKED_ENCODING.
     */
    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    /**
     * Extracts the access token using cookie-first, header-fallback strategy.
     *
     * <p>Cookie-first is preferred because HttpOnly cookies cannot be stolen
     * via XSS, unlike Authorization headers which require JavaScript access
     * to the token value.
     */
    private String extractToken(HttpServletRequest request) {
        // 1. Try HttpOnly cookie (browser clients)
        String cookieToken = extractFromCookie(request);
        if (cookieToken != null) {
            return cookieToken;
        }

        // 2. Fallback: Authorization header (API clients, Swagger, mobile)
        return extractFromHeader(request);
    }

    private String extractFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie cookie : cookies) {
            if (ACCESS_COOKIE.equals(cookie.getName()) && StringUtils.hasText(cookie.getValue())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private String extractFromHeader(HttpServletRequest request) {
        String header = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}