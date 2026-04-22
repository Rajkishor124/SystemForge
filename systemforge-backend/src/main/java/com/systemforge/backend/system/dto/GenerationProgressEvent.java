package com.systemforge.backend.system.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

/**
 * Structured SSE event payload for generation progress.
 *
 * <p>Event types:
 * <ul>
 *   <li>{@code step_started} — pipeline step is beginning execution</li>
 *   <li>{@code step_completed} — pipeline step finished successfully</li>
 *   <li>{@code step_failed} — pipeline step failed (generation continues)</li>
 *   <li>{@code progress} — overall progress percentage update</li>
 *   <li>{@code completed} — generation finished, result available via REST</li>
 *   <li>{@code failed} — generation failed entirely</li>
 * </ul>
 *
 * <p>Design decision: SSE carries metadata only, never the full result JSON.
 * The client fetches the result via {@code GET /api/v1/systems/jobs/{jobId}}
 * after receiving the {@code completed} event. This prevents data loss on
 * connection drops and scales better under concurrent users.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GenerationProgressEvent {

    /** Event type discriminator. */
    private final String type;

    /** Pipeline step name (null for progress/completed/failed events). */
    private final String step;

    /** Step ordinal (1-based). */
    private final Integer order;

    /** Total number of pipeline steps. */
    private final Integer totalSteps;

    /** Overall progress percentage (0–100). */
    private final Integer progress;

    /** Step output summary (only on step_completed). */
    private final String output;

    /** Error message (only on step_failed / failed). */
    private final String errorMessage;

    /** Step execution duration in milliseconds. */
    private final Long durationMs;

    /** Job ID reference (only on completed event). */
    private final String jobId;

    // ─── Factory Methods ──────────────────────────────────────────────────

    public static GenerationProgressEvent stepStarted(String step, int order, int totalSteps) {
        return GenerationProgressEvent.builder()
                .type("step_started")
                .step(step)
                .order(order)
                .totalSteps(totalSteps)
                .build();
    }

    public static GenerationProgressEvent stepCompleted(String step, int order, int totalSteps,
                                                         String output, long durationMs) {
        return GenerationProgressEvent.builder()
                .type("step_completed")
                .step(step)
                .order(order)
                .totalSteps(totalSteps)
                .output(output)
                .durationMs(durationMs)
                .build();
    }

    public static GenerationProgressEvent stepFailed(String step, int order, int totalSteps,
                                                      String errorMessage, long durationMs) {
        return GenerationProgressEvent.builder()
                .type("step_failed")
                .step(step)
                .order(order)
                .totalSteps(totalSteps)
                .errorMessage(errorMessage)
                .durationMs(durationMs)
                .build();
    }

    public static GenerationProgressEvent progress(int progressPercent) {
        return GenerationProgressEvent.builder()
                .type("progress")
                .progress(progressPercent)
                .build();
    }

    public static GenerationProgressEvent completed(String jobId) {
        return GenerationProgressEvent.builder()
                .type("completed")
                .jobId(jobId)
                .progress(100)
                .build();
    }

    public static GenerationProgressEvent failed(String jobId, String errorMessage) {
        return GenerationProgressEvent.builder()
                .type("failed")
                .jobId(jobId)
                .errorMessage(errorMessage)
                .build();
    }
}
