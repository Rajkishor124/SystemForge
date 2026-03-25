package com.systemforge.backend.auth.service;

import com.systemforge.backend.common.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

/**
 * Enterprise-grade JWT service.
 */
@Service
@Slf4j
public class JwtService {

    public static final String CLAIM_USER_ID = "userId";
    public static final String CLAIM_ROLE    = "role";
    public static final String CLAIM_EMAIL   = "email";
    public static final String TOKEN_TYPE    = "type";
    public static final String TYPE_ACCESS   = "ACCESS";
    public static final String TYPE_REFRESH  = "REFRESH";

    private final SecretKey signingKey;
    private final long accessTokenExpiryMs;
    private final long refreshTokenExpiryMs;

    public JwtService(AppProperties appProperties) {
        var jwt = appProperties.getJwt();

        this.signingKey = Keys.hmacShaKeyFor(
                jwt.getSecret().getBytes(StandardCharsets.UTF_8));

        this.accessTokenExpiryMs  = jwt.getAccessTokenExpiryMs();
        this.refreshTokenExpiryMs = jwt.getRefreshTokenExpiryMs();
    }

    // ================= GENERATION =================

    public String generateAccessToken(String userId, String role, String email) {
        Instant now = Instant.now();

        return Jwts.builder()
                .subject(userId)
                .claims(Map.of(
                        CLAIM_ROLE, role,
                        CLAIM_EMAIL, email,
                        TOKEN_TYPE, TYPE_ACCESS
                ))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(accessTokenExpiryMs)))
                .signWith(signingKey)
                .compact();
    }

    public String generateRefreshToken(String userId) {
        Instant now = Instant.now();

        return Jwts.builder()
                .subject(userId)
                .claims(Map.of(
                        TOKEN_TYPE, TYPE_REFRESH
                ))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(refreshTokenExpiryMs)))
                .signWith(signingKey)
                .compact();
    }

    // ================= PARSING =================

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // ================= EXTRACTORS =================

    public String extractUserId(Claims claims) {
        return claims.getSubject();
    }

    public String extractRole(Claims claims) {
        return claims.get(CLAIM_ROLE, String.class);
    }

    public String extractEmail(Claims claims) {
        return claims.get(CLAIM_EMAIL, String.class);
    }

    public Instant extractExpiry(Claims claims) {
        return claims.getExpiration().toInstant();
    }

    // ================= VALIDATION =================

    public boolean isTokenValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.debug("JWT expired");
            return false;
        } catch (JwtException e) {
            log.warn("JWT validation failed: {}", e.getClass().getSimpleName());
            return false;
        }
    }

    public boolean isAccessToken(Claims claims) {
        return TYPE_ACCESS.equals(claims.get(TOKEN_TYPE, String.class));
    }

    public boolean isRefreshToken(Claims claims) {
        return TYPE_REFRESH.equals(claims.get(TOKEN_TYPE, String.class));
    }

    public long getAccessTokenExpiryMs() {
        return accessTokenExpiryMs;
    }
}