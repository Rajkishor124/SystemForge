package com.systemforge.backend.architect.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A single message in an architect session (user or assistant).
 */
@Entity
@Table(name = "architect_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArchitectMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    /** "user" or "assistant". */
    @Column(nullable = false, length = 20)
    private String role;

    /** Message content (markdown for assistant). */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /** Response source: AI, RULE_ENGINE, FALLBACK. */
    @Column(length = 30)
    private String source;

    /** Detected intent for this message. */
    @Column(length = 50)
    private String intent;

    /** Processing time in ms (assistant messages only). */
    @Column(name = "processing_time_ms")
    private Long processingTimeMs;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
