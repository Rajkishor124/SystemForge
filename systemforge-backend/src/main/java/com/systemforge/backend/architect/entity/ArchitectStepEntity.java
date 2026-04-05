package com.systemforge.backend.architect.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Records a single decision step executed by the agent.
 *
 * <p>Provides full audit trail of the agent's reasoning pipeline.
 */
@Entity
@Table(name = "architect_steps")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArchitectStepEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "message_id", nullable = false)
    private UUID messageId;

    /** Step name (e.g., "RequirementAnalysis", "DatabaseDesign"). */
    @Column(name = "step_name", nullable = false, length = 100)
    private String stepName;

    /** Execution order. */
    @Column(name = "step_order", nullable = false)
    private int stepOrder;

    /** Status: COMPLETED, FAILED, SKIPPED. */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "COMPLETED";

    /** Step output (JSON or text). */
    @Column(columnDefinition = "TEXT")
    private String output;

    /** Duration in milliseconds. */
    @Column(name = "duration_ms")
    private long durationMs;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
