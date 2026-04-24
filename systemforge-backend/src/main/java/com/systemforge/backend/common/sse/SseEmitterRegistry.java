package com.systemforge.backend.common.sse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.systemforge.backend.common.metrics.GenerationMetrics;
import com.systemforge.backend.system.dto.GenerationProgressEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Deque;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
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
 *
 * <p>Event history is bounded to the last 50 events per job to prevent memory leaks.
 */
@Component
@Slf4j
public class SseEmitterRegistry {

    private static final int MAX_HISTORY_SIZE = 50;

    private final ObjectMapper objectMapper;
    private final GenerationMetrics metrics;

    /**
     * Active SSE connections keyed by jobId.
     * One emitter per job (1:1 mapping).
     */
    private final ConcurrentMap<UUID, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * History of events for active jobs to support client reconnection.
     * Bounded to last {@value MAX_HISTORY_SIZE} events to prevent memory leaks.
     */
    private final ConcurrentMap<UUID, Deque<Object>> eventHistory = new ConcurrentHashMap<>();

    public SseEmitterRegistry(ObjectMapper objectMapper, GenerationMetrics metrics) {
        this.objectMapper = objectMapper;
        this.metrics = metrics;
    }

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

        metrics.sseConnected();

        emitter.onCompletion(() -> {
            emitters.remove(jobId);
            metrics.sseDisconnected();
            log.debug("event=SSE_COMPLETED jobId={}", jobId);
        });
        emitter.onTimeout(() -> {
            emitters.remove(jobId);
            metrics.sseDisconnected();
            log.debug("event=SSE_TIMEOUT jobId={}", jobId);
        });
        emitter.onError(e -> {
            emitters.remove(jobId);
            metrics.sseDisconnected();
            log.debug("event=SSE_ERROR jobId={} error={}", jobId, e.getMessage());
        });

        // Replay history for reconnecting clients
        Deque<Object> history = eventHistory.get(jobId);
        if (history != null && !history.isEmpty()) {
            int replayCount = 0;
            for (Object event : history) {
                try {
                    String json = objectMapper.writeValueAsString(event);
                    String eventName = getEventName(event);
                    emitter.send(SseEmitter.event()
                            .name(eventName)
                            .data(json));
                    replayCount++;
                } catch (Exception e) {
                    log.warn("event=SSE_REPLAY_FAILED jobId={} error={}", jobId, e.getMessage());
                }
            }
            log.debug("event=SSE_REPLAY jobId={} eventsReplayed={}", jobId, replayCount);
        }

        log.info("event=SSE_REGISTERED jobId={} activeConnections={}", jobId, emitters.size());
    }

    /**
     * Sends an event to the SSE listener for a job.
     * Silently no-ops if no listener is connected.
     *
     * @param jobId the job to send the event for
     * @param event the event payload (will be serialized to JSON)
     */
    public void send(UUID jobId, Object event) {
        // Cache event for any future reconnects (bounded to MAX_HISTORY_SIZE)
        Deque<Object> history = eventHistory.computeIfAbsent(jobId, k -> new ConcurrentLinkedDeque<>());
        history.addLast(event);
        while (history.size() > MAX_HISTORY_SIZE) {
            history.pollFirst();
        }

        SseEmitter emitter = emitters.get(jobId);
        if (emitter == null) {
            log.trace("event=SSE_NO_LISTENER jobId={}", jobId);
            return;
        }

        try {
            String json = objectMapper.writeValueAsString(event);
            String eventName = getEventName(event);
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(json));
            log.debug("event=SSE_SENT jobId={} eventType={}", jobId, eventName);
        } catch (IOException e) {
            log.warn("event=SSE_SEND_FAILED jobId={} error={}", jobId, e.getMessage());
            emitters.remove(jobId);
            try { emitter.completeWithError(e); } catch (Exception ignored) {}
        }
    }

    /**
     * Completes the SSE connection for a job and clears event history.
     */
    public void complete(UUID jobId) {
        SseEmitter emitter = emitters.remove(jobId);
        eventHistory.remove(jobId);

        if (emitter == null) return;

        try {
            emitter.complete();
            log.info("event=SSE_STREAM_COMPLETED jobId={}", jobId);
        } catch (Exception e) {
            log.debug("event=SSE_COMPLETE_ERROR jobId={} error={}", jobId, e.getMessage());
        }
    }

    /**
     * Returns the number of active SSE connections.
     */
    public int activeCount() {
        return emitters.size();
    }

    /**
     * Returns the total number of events currently cached across all jobs.
     */
    public int totalCachedEvents() {
        return eventHistory.values().stream().mapToInt(Deque::size).sum();
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
                        .name("PROGRESS")
                        .data(shutdownEvent));
                emitter.complete();
                log.debug("event=SSE_SHUTDOWN jobId={}", jobId);
            } catch (Exception e) {
                log.debug("event=SSE_SHUTDOWN_ERROR jobId={} error={}", jobId, e.getMessage());
                try { emitter.completeWithError(e); } catch (Exception ignored) {}
            }
        });

        emitters.clear();
        eventHistory.clear();
    }

    /**
     * Sends heartbeat to all active SSE connections every 15 seconds.
     * Prevents TCP idle timeout disconnections by proxies/load balancers.
     */
    @Scheduled(fixedRate = 15000)
    public void sendHeartbeats() {
        if (emitters.isEmpty()) return;

        emitters.forEach((jobId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("HEARTBEAT")
                        .data("ping"));
            } catch (Exception e) {
                log.debug("event=SSE_HEARTBEAT_FAILED jobId={}", jobId);
                emitters.remove(jobId);
                try { emitter.completeWithError(e); } catch (Exception ignored) {}
            }
        });
    }

    private String getEventName(Object event) {
        if (event instanceof GenerationProgressEvent progressEvent) {
            return switch (progressEvent.getType()) {
                case "completed" -> "COMPLETED";
                case "failed" -> "FAILED";
                default -> "PROGRESS";
            };
        }
        return "PROGRESS";
    }
}
