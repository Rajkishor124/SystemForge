package com.systemforge.backend.auth.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Utility for hashing refresh tokens before database persistence.
 *
 * <p>Why hash refresh tokens in the DB?
 * A refresh token in the DB acts like a password — if the database is compromised,
 * an attacker should not be able to extract raw tokens and impersonate users.
 * SHA-256 (without salt) is appropriate here because:
 * <ul>
 *   <li>Tokens are already cryptographically random (UUID/JWT-based) — no salt needed</li>
 *   <li>We only ever look up by exact token match, not enumerate/brute-force</li>
 *   <li>SHA-256 is fast for single lookups (unlike BCrypt) which is what we need here</li>
 * </ul>
 *
 * <p>BCrypt would be wrong here — we need consistent hash output for equality lookups.
 * BCrypt uses random salts, so the same input produces different outputs each time.
 */
@UtilityClass
@Slf4j
public class TokenHashUtil {

    private static final String ALGORITHM = "SHA-256";

    /**
     * Returns the SHA-256 hex digest of the given token string.
     *
     * @param rawToken the raw refresh token JWT string
     * @return 64-character lowercase hex string
     */
    public static String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            byte[] hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed by the Java spec — this cannot happen in practice
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}