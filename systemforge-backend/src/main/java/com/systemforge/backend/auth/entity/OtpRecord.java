package com.systemforge.backend.auth.entity;

import com.systemforge.backend.common.entity.BaseEntity;
import com.systemforge.backend.common.enums.AuthType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Tracks OTP issuance for authentication flows.
 *
 * <p>Design decisions:
 * <ul>
 *   <li>OTP is stored as a BCrypt hash — never plaintext, even in DB</li>
 *   <li>{@code attempts} field enforces brute-force protection at service layer</li>
 *   <li>Records are soft-deleted after use, preserving audit trail</li>
 *   <li>Index on {@code email + is_used + expires_at} for efficient lookup during verification</li>
 * </ul>
 */
@Entity
@Table(
        name = "otp_records",
        indexes = {
                @Index(name = "idx_otp_email", columnList = "email"),
                @Index(name = "idx_otp_expires", columnList = "expires_at"),
                @Index(name = "idx_otp_email_used", columnList = "email, is_used")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpRecord extends BaseEntity {

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    /** BCrypt hash of the OTP. NEVER store raw OTP. */
    @Column(name = "otp_hash", nullable = false, length = 72)
    private String otpHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_type", nullable = false, length = 30)
    private AuthType authType;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "is_used", nullable = false)
    @Builder.Default
    private boolean isUsed = false;

    /** Number of incorrect verification attempts. Max 5 before lockout. */
    @Column(name = "attempts", nullable = false)
    @Builder.Default
    private int attempts = 0;
}