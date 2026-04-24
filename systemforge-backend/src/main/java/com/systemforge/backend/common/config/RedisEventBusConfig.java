package com.systemforge.backend.common.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * Redis Pub/Sub configuration for distributed SSE event delivery.
 *
 * <p>Only activated when {@code systemforge.event-bus.type=redis}.
 * Provides the {@link RedisMessageListenerContainer} required by
 * {@link com.systemforge.backend.common.event.RedisEventBus}.
 *
 * <p>The listener container manages a dedicated thread pool for
 * receiving Redis messages, keeping the main Tomcat thread pool
 * free for HTTP requests.
 */
@Configuration
@ConditionalOnProperty(name = "systemforge.event-bus.type", havingValue = "redis")
public class RedisEventBusConfig {

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        return container;
    }
}
