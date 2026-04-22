package com.systemforge.backend.common.sse;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Lifecycle hook that gracefully closes all SSE connections on shutdown.
 *
 * <p>When the application receives SIGTERM (e.g., during a rolling deployment),
 * this hook:
 * <ol>
 *   <li>Sends a {@code server_shutdown} event to all connected clients</li>
 *   <li>Completes each emitter so the browser's EventSource fires a close event</li>
 *   <li>Logs the count of terminated connections</li>
 * </ol>
 *
 * <p>This prevents clients from hanging indefinitely on a dead connection.
 * The frontend's {@code useGenerationStream} hook detects the closure
 * and falls back to polling automatically.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SseShutdownHook {

    private final SseEmitterRegistry sseRegistry;

    @PreDestroy
    public void onShutdown() {
        int activeCount = sseRegistry.activeCount();
        if (activeCount == 0) {
            log.info("[SSE_SHUTDOWN] No active SSE connections to close");
            return;
        }

        log.info("[SSE_SHUTDOWN] Closing {} active SSE connection(s)...", activeCount);

        // Broadcast shutdown event to all connected clients
        sseRegistry.broadcastShutdown();

        log.info("[SSE_SHUTDOWN] All SSE connections closed gracefully");
    }
}
