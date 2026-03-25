package com.systemforge.backend.template.entity;

import com.systemforge.backend.common.entity.BaseEntity;
import com.systemforge.backend.common.enums.AppScale;
import com.systemforge.backend.common.enums.AppType;
import com.systemforge.backend.common.enums.SystemType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

/**
 * Predefined architecture template seeded by ADMINs.
 *
 * <p>Templates are curated best-practice configurations for common app types.
 * Example: "OTP Auth for Ride-Hailing Apps" bundles Auth(OTP) + recommended config.
 *
 * <p>The recommendation engine uses templates as the primary output — matching
 * user inputs (appType, appScale) against available templates.
 */
@Entity
@Table(
        name = "templates",
        indexes = {
                @Index(name = "idx_tmpl_app_type", columnList = "app_type"),
                @Index(name = "idx_tmpl_system_type", columnList = "system_type"),
                @Index(name = "idx_tmpl_scale", columnList = "app_scale")
        }
)
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Template extends BaseEntity {

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "app_type", nullable = false, length = 50)
    private AppType appType;

    @Enumerated(EnumType.STRING)
    @Column(name = "system_type", nullable = false, length = 50)
    private SystemType systemType;

    @Enumerated(EnumType.STRING)
    @Column(name = "app_scale", length = 20)
    private AppScale appScale;

    /**
     * The full configuration blueprint as a JSON blob.
     * Schema: {apiDesign: {}, dbSchema: {}, techStack: [], rationale: ""}
     */
    @Column(name = "config_json", nullable = false, columnDefinition = "TEXT")
    private String configJson;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    /** Display order for template listing UI. Lower = higher priority. */
    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private int sortOrder = 100;
}