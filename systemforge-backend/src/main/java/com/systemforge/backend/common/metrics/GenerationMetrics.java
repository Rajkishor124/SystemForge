package com.systemforge.backend.common.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Centralized Micrometer metrics for the generation pipeline.
 *
 * <p>Exposes counters, gauges, and timers that are scraped by
 * {@code /actuator/metrics} and compatible with Prometheus/Grafana.
 *
 * <p>Metric naming follows Micrometer conventions (dot-separated, lowercase):
 * <ul>
 *   <li>{@code generation.jobs.created} — total jobs submitted</li>
 *   <li>{@code generation.jobs.active} — jobs currently in PROCESSING state</li>
 *   <li>{@code generation.jobs.completed} — jobs that finished successfully</li>
 *   <li>{@code generation.jobs.failed} — jobs that exhausted retries</li>
 *   <li>{@code generation.jobs.retries} — total retry attempts across all jobs</li>
 *   <li>{@code generation.sse.connections} — active SSE emitter count</li>
 *   <li>{@code generation.jobs.duration} — timer for end-to-end job duration</li>
 * </ul>
 */
@Component
@Getter
public class GenerationMetrics {

    private final Counter jobsCreated;
    private final Counter jobsCompleted;
    private final Counter jobsFailed;
    private final Counter jobsRetries;
    private final AtomicInteger activeJobs = new AtomicInteger(0);
    private final AtomicInteger sseConnections = new AtomicInteger(0);
    private final Timer jobDuration;

    public GenerationMetrics(MeterRegistry registry) {
        this.jobsCreated = Counter.builder("generation.jobs.created")
                .description("Total generation jobs created")
                .register(registry);

        this.jobsCompleted = Counter.builder("generation.jobs.completed")
                .description("Total generation jobs completed successfully")
                .register(registry);

        this.jobsFailed = Counter.builder("generation.jobs.failed")
                .description("Total generation jobs that failed after max retries")
                .register(registry);

        this.jobsRetries = Counter.builder("generation.jobs.retries")
                .description("Total retry attempts across all jobs")
                .register(registry);

        this.jobDuration = Timer.builder("generation.jobs.duration")
                .description("End-to-end duration of generation jobs")
                .register(registry);

        Gauge.builder("generation.jobs.active", activeJobs, AtomicInteger::get)
                .description("Currently active (PROCESSING) generation jobs")
                .register(registry);

        Gauge.builder("generation.sse.connections", sseConnections, AtomicInteger::get)
                .description("Active SSE emitter connections")
                .register(registry);
    }

    public void incrementCreated() {
        jobsCreated.increment();
    }

    public void incrementCompleted() {
        jobsCompleted.increment();
        activeJobs.decrementAndGet();
    }

    public void incrementFailed() {
        jobsFailed.increment();
        activeJobs.decrementAndGet();
    }

    public void incrementRetries() {
        jobsRetries.increment();
    }

    public void markProcessing() {
        activeJobs.incrementAndGet();
    }

    public void sseConnected() {
        sseConnections.incrementAndGet();
    }

    public void sseDisconnected() {
        sseConnections.decrementAndGet();
    }
}
