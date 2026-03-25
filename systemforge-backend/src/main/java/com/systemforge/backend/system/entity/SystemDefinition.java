package com.systemforge.backend.system.entity;

import com.systemforge.backend.common.entity.BaseEntity;
import com.systemforge.backend.common.enums.SystemType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

/**
 * Catalog entry representing a backend system type available in SystemForge.
 *
 * <p>Examples: Auth (OTP), Auth (JWT), Payment (Razorpay), Notification (Firebase), etc.
 * These are seeded by ADMINs and referenced by users when designing their architecture.
 *
 * <p>Stored config schema is JSON — allows flexible, type-specific configuration
 * without requiring schema changes as new system variants are added.
 */
@Entity
@Table(
        name = "system_definitions",
        indexes = {
                @Index(name = "idx_system_def_type", columnList = "system_type"),
                @Index(name = "idx_system_def_is_deleted", columnList = "is_deleted")
        }
)
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemDefinition extends BaseEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "system_type", nullable = false, length = 50)
    private SystemType systemType;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    /**
     * JSON schema defining what configuration options this system accepts.
     * E.g., for AUTH: {"authType": "OTP|EMAIL_PASSWORD", "otpExpiry": "integer"}.
     * Parsed and validated by the System Engine at config time.
     */
    @Column(name = "config_schema", columnDefinition = "TEXT")
    private String configSchema;

    /** Whether this system definition is visible and selectable by developers. */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;
}