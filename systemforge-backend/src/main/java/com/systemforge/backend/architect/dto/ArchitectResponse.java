package com.systemforge.backend.architect.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Structured response from the AI Architect Agent.
 *
 * <p>Carries both the human-readable reply AND the
 * structured reasoning chain for UI visualization.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArchitectResponse {

    /** Session ID (for continuing the conversation). */
    private UUID sessionId;

    /** Human-readable markdown reply. */
    private String reply;

    /** The classified intent of the user's request. */
    private String intent;

    /** Source of the response (AI, RULE_ENGINE, FALLBACK). */
    private String source;

    /** Step-by-step reasoning chain (visible to user). */
    private List<AgentStep> reasoningSteps;

    /** Total processing time in milliseconds. */
    private long processingTimeMs;

    /** Timestamp. */
    private LocalDateTime createdAt;
}
