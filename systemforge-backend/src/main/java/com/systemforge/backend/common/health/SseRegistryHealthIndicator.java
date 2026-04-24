package com.systemforge.backend.common.health;

import com.systemforge.backend.common.sse.SseEmitterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Custom health indicator for the SSE emitter registry.
 *
 * <p>Reports the number of active SSE connections and cached replay events.
 * Flags degraded state when connection count exceeds safe thresholds.
 *
 * <p>Exposed at: {@code GET /actuator/health} → {@code components.sseRegistry}
 */
@Component("sseRegistry")
@RequiredArgsConstructor
public class SseRegistryHealthIndicator implements HealthIndicator {

    private static final int WARNING_THRESHOLD = 200;
    private static final int CRITICAL_THRESHOLD = 500;

    private final SseEmitterRegistry sseRegistry;

    @Override
    public Health health() {
        int activeConnections = sseRegistry.activeCount();
        int cachedEvents = sseRegistry.totalCachedEvents();

        Health.Builder builder = Health.up()
                .withDetail("activeConnections", activeConnections)
                .withDetail("cachedReplayEvents", cachedEvents)
                .withDetail("maxHistoryPerJob", 50);

        if (activeConnections >= CRITICAL_THRESHOLD) {
            return builder
                    .status("DOWN")
                    .withDetail("warning", "SSE connections at critical level — risk of resource exhaustion")
                    .build();
        }

        if (activeConnections >= WARNING_THRESHOLD) {
            return builder
                    .withDetail("warning", "High SSE connection count — consider scaling")
                    .build();
        }

        return builder.build();
    }
}
