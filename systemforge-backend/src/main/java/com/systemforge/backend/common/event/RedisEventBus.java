package com.systemforge.backend.common.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.systemforge.backend.common.sse.SseEmitterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.UUID;

/**
 * Redis Pub/Sub-backed EventBus for horizontal scaling.
 *
 * <p>Enables SSE event delivery across multiple application instances.
 * When a worker on Instance A publishes an event for jobId X,
 * all instances (A, B, C...) receive it via Redis Pub/Sub and
 * forward to their local SSE connections.
 *
 * <p>Channel naming: {@code sse:events} (single channel, job routing via payload).
 * This avoids per-job channel overhead which doesn't scale well with Redis
 * when there are thousands of jobs.
 *
 * <p>Activation: set {@code systemforge.event-bus.type=redis} in config.
 * When not set (default), {@link LocalEventBus} is used.
 *
 * <p>Message format:
 * <pre>
 * {
 *   "action": "PUBLISH" | "COMPLETE",
 *   "jobId": "uuid-string",
 *   "payload": { ... event object ... }
 * }
 * </pre>
 *
 * <p>Thread safety: Redis listener runs on a dedicated thread pool.
 * The SseEmitterRegistry is thread-safe (ConcurrentHashMap-backed).
 *
 * <p>Deduplication: not required — each instance only forwards events
 * to its own local SSE connections. No client sees duplicates.
 */
@Component
@ConditionalOnProperty(name = "systemforge.event-bus.type", havingValue = "redis")
@Primary
@Slf4j
public class RedisEventBus implements EventBus {

    private static final String CHANNEL = "sse:events";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final SseEmitterRegistry sseRegistry;
    private final RedisMessageListenerContainer listenerContainer;

    public RedisEventBus(StringRedisTemplate redisTemplate,
                         ObjectMapper objectMapper,
                         SseEmitterRegistry sseRegistry,
                         RedisMessageListenerContainer listenerContainer) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.sseRegistry = sseRegistry;
        this.listenerContainer = listenerContainer;
    }

    @PostConstruct
    public void init() {
        // Subscribe to the SSE events channel
        MessageListenerAdapter adapter = new MessageListenerAdapter(this, "onMessage");
        adapter.afterPropertiesSet();
        listenerContainer.addMessageListener(adapter, new ChannelTopic(CHANNEL));

        log.info("event=REDIS_EVENT_BUS_INITIALIZED channel={}", CHANNEL);
    }

    @Override
    public void publish(UUID jobId, Object event) {
        try {
            RedisEventMessage message = new RedisEventMessage(
                    "PUBLISH",
                    jobId.toString(),
                    objectMapper.writeValueAsString(event)
            );
            String json = objectMapper.writeValueAsString(message);
            redisTemplate.convertAndSend(CHANNEL, json);
            log.debug("event=REDIS_PUBLISH jobId={} channel={}", jobId, CHANNEL);
        } catch (Exception e) {
            log.error("event=REDIS_PUBLISH_FAILED jobId={} error={}", jobId, e.getMessage());
            // Fallback: deliver locally even if Redis fails
            sseRegistry.send(jobId, event);
        }
    }

    @Override
    public void complete(UUID jobId) {
        try {
            RedisEventMessage message = new RedisEventMessage(
                    "COMPLETE",
                    jobId.toString(),
                    null
            );
            String json = objectMapper.writeValueAsString(message);
            redisTemplate.convertAndSend(CHANNEL, json);
            log.debug("event=REDIS_COMPLETE jobId={} channel={}", jobId, CHANNEL);
        } catch (Exception e) {
            log.error("event=REDIS_COMPLETE_FAILED jobId={} error={}", jobId, e.getMessage());
            // Fallback: complete locally
            sseRegistry.complete(jobId);
        }
    }

    /**
     * Redis message listener callback.
     * Called on a dedicated listener thread for each message on the channel.
     */
    @SuppressWarnings("unused") // Called reflectively by MessageListenerAdapter
    public void onMessage(String messageBody) {
        try {
            RedisEventMessage message = objectMapper.readValue(messageBody, RedisEventMessage.class);
            UUID jobId = UUID.fromString(message.jobId());

            switch (message.action()) {
                case "PUBLISH" -> {
                    // Deserialize and forward to local SSE connections
                    Object event = objectMapper.readValue(message.payload(), Object.class);
                    sseRegistry.send(jobId, event);
                    log.debug("event=REDIS_RECEIVED action=PUBLISH jobId={}", jobId);
                }
                case "COMPLETE" -> {
                    sseRegistry.complete(jobId);
                    log.debug("event=REDIS_RECEIVED action=COMPLETE jobId={}", jobId);
                }
                default -> log.warn("event=REDIS_UNKNOWN_ACTION action={} jobId={}", message.action(), jobId);
            }
        } catch (Exception e) {
            log.error("event=REDIS_MESSAGE_PARSE_FAILED error={}", e.getMessage());
        }
    }

    /**
     * Internal message envelope for Redis Pub/Sub transport.
     */
    record RedisEventMessage(String action, String jobId, String payload) {}
}
