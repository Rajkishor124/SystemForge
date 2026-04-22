package com.systemforge.backend.common.config;

import org.slf4j.MDC;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
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
 *   <li>CallerRunsPolicy — under extreme load, the calling thread runs the
 *       task itself rather than rejecting it (graceful degradation)</li>
 * </ul>
 */
@Configuration
@EnableAsync
@Slf4j
public class AsyncConfig implements AsyncConfigurer {

    /**
     * Dedicated executor for AI generation tasks.
     *
     * <p>Sizing rationale:
     * <ul>
     *   <li>Core 10: handles typical concurrent generation requests</li>
     *   <li>Max 50: burst capacity during peak hours</li>
     *   <li>Queue 100: buffer for spikes; rejected tasks fall back to caller thread</li>
     * </ul>
     */
    @Bean("aiGenerationExecutor")
    public Executor aiGenerationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("ai-gen-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setTaskDecorator(new MdcTaskDecorator());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, params) ->
                log.error("[ASYNC] Uncaught exception in {}: {}", method.getName(), throwable.getMessage(), throwable);
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
