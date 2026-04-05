package com.systemforge.backend.architect.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents an AI Architect design session.
 *
 * <p>Each session holds a sequence of messages, reasoning steps,
 * and tool invocations — providing full observability of the agent's work.
 */
@Entity
@Table(name = "architect_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArchitectSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 200)
    @Builder.Default
    private String title = "New Design Session";

    /** Detected intent: SYSTEM_DESIGN, ANALYSIS, QUESTION, etc. */
    @Column(length = 50)
    private String intent;

    /** Status: ACTIVE, COMPLETED, ARCHIVED. */
    @Column(length = 20, nullable = false)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private boolean deleted = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
