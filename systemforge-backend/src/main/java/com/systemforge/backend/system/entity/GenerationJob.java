package com.systemforge.backend.system.entity;

import com.systemforge.backend.common.entity.BaseEntity;
import com.systemforge.backend.common.enums.JobStatus;
import com.systemforge.backend.common.enums.JobType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Tracks an async AI generation task.
 *
 * <p>Decouples long-running LLM pipeline execution from the HTTP request thread.
 * The controller returns a job reference immediately (202 Accepted), and the
 * client polls or subscribes for completion.
 *
 * <p>Design decisions:
 * <ul>
 *   <li>{@code configId} is nullable — architect chat jobs don't have a config</li>
 *   <li>{@code sessionId} is nullable — system generation jobs don't have a session</li>
 *   <li>{@code resultJson} stores the full output; a future optimization may
 *       store a reference to an external blob store for very large payloads</li>
 *   <li>{@code mabaMetadata} (JSONB) stores per-agent execution data for
 *       cost auditing, performance debugging, and failure forensics</li>
 * </ul>
 */
@Entity
@Table(
        name = "generation_jobs",
        indexes = {
                @Index(name = "idx_gj_user_id", columnList = "user_id"),
                @Index(name = "idx_gj_status", columnList = "status"),
                @Index(name = "idx_gj_config_id", columnList = "config_id"),
                @Index(name = "idx_gj_created", columnList = "created_at")
        }
)
@SQLRestriction("is_deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GenerationJob extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "config_id")
    private UUID configId;

    @Column(name = "session_id")
    private UUID sessionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", nullable = false, length = 30)
    @Builder.Default
    private JobType jobType = JobType.SYSTEM_GENERATION;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private JobStatus status = JobStatus.PENDING;

    @Column(name = "result_json", columnDefinition = "TEXT")
    private String resultJson;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /**
     * MABA pipeline execution metadata stored as JSONB.
     * Contains per-agent token usage, timing, status, and pipeline traceId.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "maba_metadata", columnDefinition = "jsonb")
    private String mabaMetadata;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "max_retries", nullable = false)
    @Builder.Default
    private Integer maxRetries = 3;
}

