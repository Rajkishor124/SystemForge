package com.systemforge.backend.architect.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Records a tool invocation made by the agent during reasoning.
 */
@Entity
@Table(name = "architect_tool_invocations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ToolInvocationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "message_id", nullable = false)
    private UUID messageId;

    /** Tool name (e.g., "LoadEstimator", "DatabaseSelector"). */
    @Column(name = "tool_name", nullable = false, length = 100)
    private String toolName;

    /** Input parameters (JSON). */
    @Column(name = "input_json", columnDefinition = "TEXT")
    private String inputJson;

    /** Output result (JSON). */
    @Column(name = "output_json", columnDefinition = "TEXT")
    private String outputJson;

    /** Status: SUCCESS, FAILED, TIMEOUT. */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "SUCCESS";

    /** Duration in milliseconds. */
    @Column(name = "duration_ms")
    private long durationMs;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
