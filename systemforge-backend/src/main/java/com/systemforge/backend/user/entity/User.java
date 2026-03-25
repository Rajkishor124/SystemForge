package com.systemforge.backend.user.entity;

import com.systemforge.backend.common.entity.BaseEntity;
import com.systemforge.backend.common.enums.UserRole;
import com.systemforge.backend.user.enums.AccountStatus;
import com.systemforge.backend.user.enums.AuthProvider;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;

/**
 * Core User entity for SystemForge.
 *
 * Design Notes:
 * - Uses email as the primary identity (no phone number).
 * - Supports multiple authentication providers (LOCAL, GOOGLE, EMAIL_OTP).
 * - Soft delete is enforced via @SQLRestriction.
 * - Password is stored as BCrypt hash (nullable for OAuth users).
 * - Designed for scalability and future microservice extraction.
 */
@Entity
@Table(
        name = "users",
        indexes = {
                @Index(name = "idx_users_email", columnList = "email"),
                @Index(name = "idx_users_role", columnList = "role"),
                @Index(name = "idx_users_status_role", columnList = "account_status, role"),
                @Index(name = "idx_users_is_deleted", columnList = "is_deleted")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_users_email", columnNames = "email")
        }
)
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "password")
public class User extends BaseEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * Email is the primary identifier.
     * Must be stored in lowercase for consistency.
     */
    @Column(name = "email", nullable = false, length = 255)
    private String email;

    /**
     * BCrypt-hashed password.
     * Nullable for OAuth (Google) or OTP-only users.
     */
    @Column(name = "password", length = 72)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private UserRole role;

    /**
     * Account lifecycle state.
     * Replaces simple boolean isActive for better control.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false, length = 20)
    private AccountStatus accountStatus;

    /**
     * Authentication provider type.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider", nullable = false, length = 20)
    private AuthProvider authProvider;

    /**
     * Whether email has been verified.
     */
    @Column(name = "is_email_verified", nullable = false)
    @Builder.Default
    private boolean isEmailVerified = false;

    /**
     * Last successful login timestamp.
     */
    @Column(name = "last_login_at")
    private Instant lastLoginAt;
}