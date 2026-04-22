package com.systemforge.backend.common.sse;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Thread-safe registry for active SSE connections.
 *
 * <p>Lifecycle:
 * <ol>
 *   <li>Client opens {@code GET /jobs/{jobId}/stream}</li>
 *   <li>Controller creates an {@link SseEmitter} and registers it here</li>
 *   <li>Async worker calls {@link #send} as pipeline steps execute</li>
 *   <li>On completion/failure, worker calls {@link #complete} to close the connection</li>
 *   <li>Emitter is auto-removed on timeout, error, or completion</li>
 * </ol>
 *
 * <p>If no SSE listener is connected when events are emitted (e.g., client only
 * uses polling), events are silently dropped. This makes SSE strictly optional —
 * the polling API is always the source of truth.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SseEmitterRegistry {

    private final ObjectMapper objectMapper;

    /**
     * Active SSE connections keyed by jobId.
     * One emitter per job (1:1 mapping).
     */
    private final ConcurrentMap<UUID, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * Registers an emitter for a job.
     * Sets up automatic cleanup callbacks for timeout, completion, and error.
     */
    public void register(UUID jobId, SseEmitter emitter) {
        // Clean up any stale emitter for this job
        SseEmitter existing = emitters.put(jobId, emitter);
        if (existing != null) {
            try { existing.complete(); } catch (Exception ignored) {}
        }

        emitter.onCompletion(() -> {
            emitters.remove(jobId);
            log.debug("[SSE] Emitter completed for jobId={}", jobId);
        });
        emitter.onTimeout(() -> {
            emitters.remove(jobId);
            log.debug("[SSE] Emitter timed out for jobId={}", jobId);
        });
        emitter.onError(e -> {
            emitters.remove(jobId);
            log.debug("[SSE] Emitter error for jobId={}: {}", jobId, e.getMessage());
        });

        log.info("[SSE] Registered emitter for jobId={}", jobId);
    }

    /**
     * Sends an event to the SSE listener for a job.
     * Silently no-ops if no listener is connected.
     *
     * @param jobId the job to send the event for
     * @param event the event payload (will be serialized to JSON)
     */
    public void send(UUID jobId, Object event) {
        SseEmitter emitter = emitters.get(jobId);
        if (emitter == null) {
            log.trace("[SSE] No listener for jobId={}, dropping event", jobId);
            return;
        }

        try {
            String json = objectMapper.writeValueAsString(event);
            emitter.send(SseEmitter.event()
                    .name("generation-progress")
                    .data(json));
            log.debug("[SSE] Sent event for jobId={}: {}", jobId, json.substring(0, Math.min(json.length(), 100)));
        } catch (IOException e) {
            log.warn("[SSE] Failed to send event for jobId={}: {}", jobId, e.getMessage());
            emitters.remove(jobId);
            try { emitter.completeWithError(e); } catch (Exception ignored) {}
        }
    }

    /**
     * Completes the SSE connection for a job.
     * Sends a final event before closing if provided.
     */
    public void complete(UUID jobId) {
        SseEmitter emitter = emitters.remove(jobId);
        if (emitter == null) return;

        try {
            emitter.complete();
            log.info("[SSE] Completed stream for jobId={}", jobId);
        } catch (Exception e) {
            log.debug("[SSE] Error completing emitter for jobId={}: {}", jobId, e.getMessage());
        }
    }

    /**
     * Returns the number of active SSE connections.
     * Useful for monitoring/metrics.
     */
    public int activeCount() {
        return emitters.size();
    }

    /**
     * Sends a server_shutdown event to all connected clients and closes all emitters.
     * Called during graceful application shutdown to prevent client-side hanging.
     */
    public void broadcastShutdown() {
        String shutdownEvent = "{\"type\":\"server_shutdown\"}";

        emitters.forEach((jobId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("generation-progress")
                        .data(shutdownEvent));
                emitter.complete();
                log.debug("[SSE] Shutdown: closed emitter for jobId={}", jobId);
            } catch (Exception e) {
                log.debug("[SSE] Shutdown: error closing emitter for jobId={}: {}", jobId, e.getMessage());
                try { emitter.completeWithError(e); } catch (Exception ignored) {}
            }
        });

        emitters.clear();
    }
}
