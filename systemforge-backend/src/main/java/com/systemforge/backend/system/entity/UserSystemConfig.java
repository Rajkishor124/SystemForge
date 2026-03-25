package com.systemforge.backend.system.entity;

import com.systemforge.backend.common.entity.BaseEntity;
import com.systemforge.backend.common.enums.AppScale;
import com.systemforge.backend.common.enums.AppType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;

/**
 * Persists a user's architecture design session.
 *
 * <p>Captures the full context of what a developer has designed:
 * <ul>
 *   <li>Which app type they're building</li>
 *   <li>Target scale</li>
 *   <li>Which systems they selected and how they configured them</li>
 *   <li>The generated output (API design, DB schema, tech stack)</li>
 * </ul>
 *
 * <p>{@code selectedSystemsJson} and {@code generatedOutputJson} store the
 * structured data as JSON strings. A future iteration may extract these
 * into typed child entities as the schema stabilizes.
 *
 * <p>userId is stored as a UUID column (not a JPA @ManyToOne join) because
 * cross-module entity relationships are prohibited by architecture rules —
 * communication happens via service interfaces only.
 */
@Entity
@Table(
        name = "user_system_configs",
        indexes = {
                @Index(name = "idx_usc_user_id", columnList = "user_id"),
                @Index(name = "idx_usc_app_type", columnList = "app_type"),
                @Index(name = "idx_usc_is_deleted", columnList = "is_deleted")
        }
)
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSystemConfig extends BaseEntity {

    /**
     * Reference to the owning user.
     *
     * <p>Stored as a plain UUID column, NOT a {@code @ManyToOne} join to {@code User}.
     * This preserves module isolation — the System module has no compile-time
     * dependency on the User module's entity.
     */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "config_name", nullable = false, length = 150)
    private String configName;

    @Enumerated(EnumType.STRING)
    @Column(name = "app_type", nullable = false, length = 50)
    private AppType appType;

    @Enumerated(EnumType.STRING)
    @Column(name = "app_scale", nullable = false, length = 20)
    private AppScale appScale;

    /**
     * JSON array of selected system IDs and their custom configurations.
     * Schema: [{systemId: UUID, customConfig: {}}]
     */
    @Column(name = "selected_systems_json", nullable = false, columnDefinition = "TEXT")
    private String selectedSystemsJson;

    /**
     * JSON blob of the generated architecture output.
     * Schema: {apiDesign: {}, dbSchema: {}, techStack: [], explanation: ""}
     * Populated after the user triggers generation.
     */
    @Column(name = "generated_output_json", columnDefinition = "TEXT")
    private String generatedOutputJson;

    /** Whether generation has been triggered and output is available. */
    @Column(name = "is_generated", nullable = false)
    @Builder.Default
    private boolean isGenerated = false;
}