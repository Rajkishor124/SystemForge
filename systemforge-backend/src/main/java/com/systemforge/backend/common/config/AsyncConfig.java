package com.systemforge.backend.common.config;

import com.systemforge.backend.common.sse.SseEmitterRegistry;
import org.slf4j.MDC;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import lombok.extern.slf4j.Slf4j;

/**
 * Async configuration for background task execution.
 *
 * <p>Key design decisions:
 * <ul>
 *   <li>Dedicated thread pool for AI generation — prevents starvation of
 *       the Tomcat request thread pool during heavy LLM workloads</li>
 *   <li>MDC-propagating TaskDecorator — ensures correlationId flows from
 *       the HTTP request thread into the async worker thread</li>
 *   <li>AbortPolicy — under extreme load, rejects new tasks immediately
 *       rather than blocking the caller (backpressure handled upstream)</li>
 *   <li>Scheduled pool stats logging — periodic visibility into thread utilization</li>
 *   <li>Pool sizes externalized via application-{profile}.yaml</li>
 * </ul>
 */
@Configuration
@EnableAsync
@EnableScheduling
@Slf4j
public class AsyncConfig implements AsyncConfigurer {

    private ThreadPoolTaskExecutor aiExecutor;
    private final SseEmitterRegistry sseRegistry;

    @Value("${systemforge.async.core-pool-size:10}")
    private int corePoolSize;

    @Value("${systemforge.async.max-pool-size:50}")
    private int maxPoolSize;

    @Value("${systemforge.async.queue-capacity:100}")
    private int queueCapacity;

    public AsyncConfig(SseEmitterRegistry sseRegistry) {
        this.sseRegistry = sseRegistry;
    }

    /**
     * Dedicated executor for AI generation tasks.
     *
     * <p>Sizing is profile-driven via {@code systemforge.async.*} properties.
     * Dev defaults are conservative; production values scale to handle
     * concurrent generation workloads.
     */
    @Bean("aiGenerationExecutor")
    public Executor aiGenerationExecutor() {
        aiExecutor = new ThreadPoolTaskExecutor();
        aiExecutor.setCorePoolSize(corePoolSize);
        aiExecutor.setMaxPoolSize(maxPoolSize);
        aiExecutor.setQueueCapacity(queueCapacity);
        aiExecutor.setThreadNamePrefix("ai-gen-");
        aiExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        aiExecutor.setTaskDecorator(new MdcTaskDecorator());
        aiExecutor.setWaitForTasksToCompleteOnShutdown(true);
        aiExecutor.setAwaitTerminationSeconds(30);
        aiExecutor.initialize();

        log.info("event=EXECUTOR_INITIALIZED pool=aiGenerationExecutor core={} max={} queue={}",
                corePoolSize, maxPoolSize, queueCapacity);

        return aiExecutor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, params) ->
                log.error("event=ASYNC_UNCAUGHT_EXCEPTION method={} error={}", method.getName(), throwable.getMessage(), throwable);
    }

    // ─── Pool Stats Monitoring ────────────────────────────────────────────

    /**
     * Logs thread pool utilization every 60 seconds.
     * Enables proactive detection of pool exhaustion before it causes 503s.
     */
    @Scheduled(fixedRate = 60_000, initialDelay = 30_000)
    public void logPoolStats() {
        if (aiExecutor == null) return;
        ThreadPoolExecutor pool = aiExecutor.getThreadPoolExecutor();
        log.info("event=POOL_STATS pool=ai-gen active={} poolSize={} queueSize={} completed={} sseConnections={}",
                pool.getActiveCount(),
                pool.getPoolSize(),
                pool.getQueue().size(),
                pool.getCompletedTaskCount(),
                sseRegistry.activeCount());
    }

    /**
     * Propagates SLF4J MDC context (correlationId, userId, etc.)
     * from the calling thread into the async worker thread.
     *
     * <p>Without this, all async log lines would lose their correlation ID,
     * making distributed tracing impossible.
     */
    static class MdcTaskDecorator implements TaskDecorator {

        @Override
        @NonNull
        public Runnable decorate(@NonNull Runnable runnable) {
            // Capture MDC from the calling thread
            Map<String, String> contextMap = MDC.getCopyOfContextMap();
            return () -> {
                try {
                    if (contextMap != null) {
                        MDC.setContextMap(contextMap);
                    }
                    runnable.run();
                } finally {
                    MDC.clear();
                }
            };
        }
    }
}
