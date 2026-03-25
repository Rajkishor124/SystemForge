package com.systemforge.backend.auth.entity;

import com.systemforge.backend.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Persists issued refresh tokens for rotation and revocation.
 *
 * <p>Why persist refresh tokens?
 * <ul>
 *   <li>Enables token revocation (logout, password change, suspicious activity)</li>
 *   <li>Supports single-device enforcement if needed</li>
 *   <li>Provides audit trail of active sessions</li>
 * </ul>
 *
 * <p>Token rotation strategy: on every refresh, the old token is marked {@code isRevoked = true}
 * and a new token is issued. If a revoked token is presented, it indicates token theft —
 * all tokens for that user should be revoked immediately.
 *
 * <p>userId stored as UUID column (not @ManyToOne) to preserve module isolation.
 */
@Entity
@Table(
        name = "refresh_tokens",
        indexes = {
                @Index(name = "idx_rt_token_hash", columnList = "token_hash"),
                @Index(name = "idx_rt_user_id", columnList = "user_id"),
                @Index(name = "idx_rt_expires", columnList = "expires_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /**
     * SHA-256 hash of the actual refresh token string.
     * The raw token is only ever held in memory or sent over HTTPS — never stored in DB.
     */
    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "is_revoked", nullable = false)
    @Builder.Default
    private boolean isRevoked = false;

    /** Client IP at time of issuance — for suspicious-activity detection. */
    @Column(name = "issued_ip", length = 45)
    private String issuedIp;

    /** User-agent string — for session display (e.g., "Chrome on Windows"). */
    @Column(name = "user_agent", length = 512)
    private String userAgent;
}