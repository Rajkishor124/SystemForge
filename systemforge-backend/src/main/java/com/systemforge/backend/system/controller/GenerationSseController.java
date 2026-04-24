package com.systemforge.backend.system.controller;

import com.systemforge.backend.auth.service.SecurityService;
import com.systemforge.backend.common.sse.SseEmitterRegistry;
import com.systemforge.backend.system.dto.GenerationProgressEvent;
import com.systemforge.backend.system.dto.GenerationJobDto;
import com.systemforge.backend.common.enums.JobStatus;
import com.systemforge.backend.system.service.SystemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.UUID;

/**
 * SSE controller for real-time generation progress streaming.
 *
 * <p>Clients subscribe to this endpoint after submitting a generation job.
 * Events are pushed as the AI pipeline executes each step. The final event
 * contains only the jobId — the client fetches the full result via the
 * polling REST endpoint.
 *
 * <p>Usage flow:
 * <pre>
 * 1. POST /configs/{id}/generate → 202 { jobId: "..." }
 * 2. GET /jobs/{jobId}/stream   → SSE connection opens
 * 3. Server pushes: step_started → step_completed → progress → completed
 * 4. Client receives "completed" → calls GET /jobs/{jobId} for full result
 * </pre>
 *
 * <p>SSE is strictly optional. The polling API remains the source of truth.
 * If the SSE connection drops, the client falls back to polling.
 */
@RestController
@RequestMapping("/api/v1/systems")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Generation Progress", description = "Real-time SSE stream for AI generation progress")
public class GenerationSseController {

    private static final long SSE_TIMEOUT_MS = 120_000L; // 2 minutes

    private final SseEmitterRegistry sseRegistry;
    private final SecurityService securityService;
    private final SystemService systemService;

    @GetMapping(value = "/jobs/{jobId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(
            summary = "Stream generation progress",
            description = """
                    Opens a Server-Sent Events connection for real-time generation progress.
                    Events include step_started, step_completed, progress, and completed.
                    The 'completed' event contains only the jobId — fetch full result via GET /jobs/{jobId}.
                    SSE is optional — polling GET /jobs/{jobId} always works as fallback.
                    """
    )
    public SseEmitter streamJobProgress(@PathVariable UUID jobId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        
        try {
            UUID userId = securityService.getAuthenticatedUserId();
            log.info("[SSE] Client subscribing to job stream: jobId={}, userId={}", jobId, userId);

            GenerationJobDto job = systemService.getJobStatus(userId, jobId);
            sseRegistry.register(jobId, emitter);

            if (job.getStatus() == JobStatus.COMPLETED) {
                sseRegistry.send(jobId, GenerationProgressEvent.completed(jobId.toString()));
                sseRegistry.complete(jobId);
                return emitter;
            } else if (job.getStatus() == JobStatus.FAILED) {
                sseRegistry.send(jobId, GenerationProgressEvent.failed(jobId.toString(), job.getErrorMessage()));
                sseRegistry.complete(jobId);
                return emitter;
            }

            // Send initial connection event
            emitter.send(SseEmitter.event()
                    .name("INIT")
                    .data("{\"type\":\"connected\",\"jobId\":\"" + jobId + "\"}"));
            
        } catch (Exception e) {
            log.error("[SSE] Failed to establish stream for jobId={}: {}", jobId, e.getMessage());
            try {
                emitter.send(SseEmitter.event()
                        .name("FAILED")
                        .data("{\"type\":\"failed\",\"errorMessage\":\"" + e.getMessage() + "\"}"));
                emitter.complete();
            } catch (IOException ignored) {}
        }

        return emitter;
    }
}
