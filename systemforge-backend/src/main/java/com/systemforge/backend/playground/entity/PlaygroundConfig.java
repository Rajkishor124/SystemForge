package com.systemforge.backend.playground.entity;

import com.systemforge.backend.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;

/**
 * Persisted playground configuration for history.
 */
@Entity
@Table(
        name = "playground_configs",
        indexes = {
                @Index(name = "idx_pg_configs_user_id", columnList = "user_id"),
                @Index(name = "idx_pg_configs_created_at", columnList = "created_at")
        }
)
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlaygroundConfig extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "service_type", nullable = false, length = 30)
    private String serviceType;

    @Column(name = "variant", nullable = false, length = 30)
    private String variant;

    @Column(name = "features_json", columnDefinition = "TEXT")
    private String featuresJson;

    @Column(name = "generated_output_json", columnDefinition = "TEXT")
    private String generatedOutputJson;
}
