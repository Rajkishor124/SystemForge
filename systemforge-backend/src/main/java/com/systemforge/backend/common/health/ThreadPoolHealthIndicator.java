package com.systemforge.backend.common.health;

import com.systemforge.backend.common.sse.SseEmitterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Custom health indicator for the AI generation thread pool.
 *
 * <p>Reports thread pool utilization and flags degraded states:
 * <ul>
 *   <li>{@code UP} — pool is healthy, capacity available</li>
 *   <li>{@code UP (warning)} — active threads or queue > 80% capacity</li>
 *   <li>{@code DOWN} — queue is full, new tasks will be rejected or caller-run</li>
 * </ul>
 *
 * <p>Exposed at: {@code GET /actuator/health} → {@code components.aiThreadPool}
 */
@Component("aiThreadPool")
@Slf4j
public class ThreadPoolHealthIndicator implements HealthIndicator {

    private final ThreadPoolTaskExecutor executor;
    private final SseEmitterRegistry sseRegistry;

    public ThreadPoolHealthIndicator(
            @Qualifier("aiGenerationExecutor") Executor executor,
            SseEmitterRegistry sseRegistry) {
        this.executor = (ThreadPoolTaskExecutor) executor;
        this.sseRegistry = sseRegistry;
    }

    @Override
    public Health health() {
        ThreadPoolExecutor pool = executor.getThreadPoolExecutor();

        int activeCount = pool.getActiveCount();
        int poolSize = pool.getPoolSize();
        int maxPoolSize = pool.getMaximumPoolSize();
        int queueSize = pool.getQueue().size();
        int queueCapacity = pool.getQueue().remainingCapacity() + queueSize;
        long completedTasks = pool.getCompletedTaskCount();
        int sseConnections = sseRegistry.activeCount();

        double threadUtilization = maxPoolSize > 0
                ? (double) activeCount / maxPoolSize * 100
                : 0;
        double queueUtilization = queueCapacity > 0
                ? (double) queueSize / queueCapacity * 100
                : 0;

        Health.Builder builder = Health.up()
                .withDetail("activeThreads", activeCount)
                .withDetail("poolSize", poolSize)
                .withDetail("maxPoolSize", maxPoolSize)
                .withDetail("queueSize", queueSize)
                .withDetail("queueCapacity", queueCapacity)
                .withDetail("completedTasks", completedTasks)
                .withDetail("threadUtilization", String.format("%.1f%%", threadUtilization))
                .withDetail("queueUtilization", String.format("%.1f%%", queueUtilization))
                .withDetail("sseConnections", sseConnections);

        // Queue full → tasks will be rejected or caller-run
        if (queueUtilization >= 100) {
            return builder
                    .status("DOWN")
                    .withDetail("warning", "Queue is FULL — CallerRunsPolicy active")
                    .build();
        }

        // High utilization warning
        if (threadUtilization > 80 || queueUtilization > 80) {
            return builder
                    .withDetail("warning", "High utilization — consider scaling")
                    .build();
        }

        return builder.build();
    }
}
