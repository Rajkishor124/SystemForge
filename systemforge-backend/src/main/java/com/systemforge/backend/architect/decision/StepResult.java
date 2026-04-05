package com.systemforge.backend.architect.decision;

import lombok.Builder;
import lombok.Getter;

/**
 * Result of executing a {@link DecisionStep}.
 */
@Getter
@Builder
public class StepResult {

    /** Status: COMPLETED, FAILED, SKIPPED. */
    private final String status;

    /** Human-readable summary of what was decided. */
    private final String summary;

    /** Structured data output stored in context for downstream steps. */
    private final Object structuredData;
}
