package com.systemforge.backend.common.event;

import java.util.UUID;

/**
 * Abstraction layer for publishing SSE events.
 *
 * <p>In a single-instance deployment, events are dispatched directly
 * to the in-memory {@code SseEmitterRegistry}. In a distributed
 * deployment, this interface can be backed by Redis Pub/Sub, Kafka,
 * or any message broker to fan out events to all application instances.
 *
 * <p>Usage:
 * <pre>
 * // Worker publishes events via EventBus
 * eventBus.publish(jobId, GenerationProgressEvent.progress(50));
 *
 * // EventBus implementation routes to appropriate transport
 * // Single-instance: direct call to SseEmitterRegistry.send()
 * // Multi-instance:  publish to Redis Pub/Sub channel "sse:{jobId}"
 * </pre>
 *
 * <p>Implementors:
 * <ul>
 *   <li>{@link LocalEventBus} — in-process, single-instance (default)</li>
 *   <li>{@code RedisEventBus} — Redis Pub/Sub (future, multi-instance)</li>
 * </ul>
 */
public interface EventBus {

    /**
     * Publishes an event for a specific job.
     * All SSE subscribers listening to this job will receive the event.
     *
     * @param jobId the target job
     * @param event the event payload (will be serialized to JSON)
     */
    void publish(UUID jobId, Object event);

    /**
     * Signals that a job stream is complete.
     * Closes all SSE connections for this job and clears replay history.
     *
     * @param jobId the job to complete
     */
    void complete(UUID jobId);
}
