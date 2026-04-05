package com.systemforge.backend.architect.dto;

import lombok.*;

/**
 * Represents a single reasoning step in the agent's decision pipeline.
 *
 * <p>These steps are exposed to the frontend for transparency —
 * users can see HOW the agent arrived at its design decisions.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentStep {

    /** Step name (e.g., "Requirement Analysis", "Database Design"). */
    private String name;

    /** Step order in the pipeline. */
    private int order;

    /** Status: COMPLETED, FAILED, SKIPPED. */
    private String status;

    /** Step output summary (human-readable). */
    private String output;

    /** Time taken for this step in milliseconds. */
    private long durationMs;
}
