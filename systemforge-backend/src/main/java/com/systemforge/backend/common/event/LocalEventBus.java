package com.systemforge.backend.common.event;

import com.systemforge.backend.common.sse.SseEmitterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * In-process EventBus implementation for single-instance deployments.
 *
 * <p>Delegates directly to {@link SseEmitterRegistry} — zero network overhead.
 * This is the default implementation, activated automatically via Spring DI.
 *
 * <p>For multi-instance scaling, replace with {@code RedisEventBus}:
 * <pre>
 * // Future Redis implementation would:
 * // 1. Publish event to Redis channel "sse:{jobId}"
 * // 2. Each instance subscribes to relevant channels
 * // 3. On receive, each instance calls its local SseEmitterRegistry.send()
 * </pre>
 *
 * @see EventBus
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LocalEventBus implements EventBus {

    private final SseEmitterRegistry sseRegistry;

    @Override
    public void publish(UUID jobId, Object event) {
        sseRegistry.send(jobId, event);
    }

    @Override
    public void complete(UUID jobId) {
        sseRegistry.complete(jobId);
    }
}
