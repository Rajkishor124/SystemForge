package com.systemforge.backend.common.metrics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Periodic alerting hook that evaluates system health thresholds
 * and emits structured warnings when limits are approached.
 *
 * <p>Designed for integration with monitoring tools:
 * <ul>
 *   <li>Log-based alerts (Datadog, Splunk, CloudWatch Logs)</li>
 *   <li>Micrometer-based alerts (Prometheus + Alertmanager)</li>
 *   <li>Future webhook/PagerDuty integration</li>
 * </ul>
 *
 * <p>Alert levels:
 * <ul>
 *   <li>{@code ALERT_WARN} — approaching threshold, investigate</li>
 *   <li>{@code ALERT_CRITICAL} — threshold exceeded, immediate action required</li>
 * </ul>
 */
@Component
@Slf4j
public class AlertingHook {

    private final GenerationMetrics metrics;
    private final ThreadPoolTaskExecutor executor;

    @Value("${systemforge.alerting.failure-rate-threshold:0.3}")
    private double failureRateThreshold;

    @Value("${systemforge.alerting.active-jobs-limit:40}")
    private int activeJobsLimit;

    @Value("${systemforge.alerting.sse-connections-limit:300}")
    private int sseConnectionsLimit;

    private final AtomicLong lastCheckedCreated = new AtomicLong(0);
    private final AtomicLong lastCheckedFailed = new AtomicLong(0);

    public AlertingHook(GenerationMetrics metrics,
                        @Qualifier("aiGenerationExecutor") Executor executor) {
        this.metrics = metrics;
        this.executor = (ThreadPoolTaskExecutor) executor;
    }

    /**
     * Evaluates alerting thresholds every 30 seconds.
     * Emits structured log warnings that monitoring tools can capture.
     */
    @Scheduled(fixedRate = 30_000, initialDelay = 60_000)
    public void evaluateAlerts() {
        checkFailureRate();
        checkActiveJobs();
        checkSseConnections();
        checkThreadPoolSaturation();
    }

    private void checkFailureRate() {
        double currentCreated = metrics.getJobsCreated().count();
        double currentFailed = metrics.getJobsFailed().count();

        double deltaCreated = currentCreated - lastCheckedCreated.get();
        double deltaFailed = currentFailed - lastCheckedFailed.get();

        lastCheckedCreated.set((long) currentCreated);
        lastCheckedFailed.set((long) currentFailed);

        if (deltaCreated < 5) return; // Not enough data in this window

        double failureRate = deltaFailed / deltaCreated;

        if (failureRate > failureRateThreshold) {
            log.error("event=ALERT_CRITICAL alert=HIGH_FAILURE_RATE failureRate={} threshold={} " +
                            "windowCreated={} windowFailed={} message=Job failure rate exceeds threshold",
                    String.format("%.2f", failureRate),
                    String.format("%.2f", failureRateThreshold),
                    (int) deltaCreated, (int) deltaFailed);
        }
    }

    private void checkActiveJobs() {
        int activeJobs = metrics.getActiveJobs().get();

        if (activeJobs > activeJobsLimit) {
            log.error("event=ALERT_CRITICAL alert=ACTIVE_JOBS_EXCEEDED activeJobs={} limit={} " +
                    "message=Active job count exceeds safe threshold", activeJobs, activeJobsLimit);
        } else if (activeJobs > activeJobsLimit * 0.8) {
            log.warn("event=ALERT_WARN alert=ACTIVE_JOBS_HIGH activeJobs={} limit={} " +
                    "message=Active job count approaching threshold", activeJobs, activeJobsLimit);
        }
    }

    private void checkSseConnections() {
        int connections = metrics.getSseConnections().get();

        if (connections > sseConnectionsLimit) {
            log.error("event=ALERT_CRITICAL alert=SSE_CONNECTIONS_EXCEEDED connections={} limit={} " +
                    "message=SSE connection count exceeds safe threshold", connections, sseConnectionsLimit);
        } else if (connections > sseConnectionsLimit * 0.8) {
            log.warn("event=ALERT_WARN alert=SSE_CONNECTIONS_HIGH connections={} limit={} " +
                    "message=SSE connections approaching threshold", connections, sseConnectionsLimit);
        }
    }

    private void checkThreadPoolSaturation() {
        ThreadPoolExecutor pool = executor.getThreadPoolExecutor();
        int queueSize = pool.getQueue().size();
        int queueCapacity = pool.getQueue().remainingCapacity() + queueSize;

        if (queueCapacity > 0) {
            double utilization = (double) queueSize / queueCapacity;
            if (utilization > 0.9) {
                log.error("event=ALERT_CRITICAL alert=THREAD_POOL_SATURATED queueUsed={} queueCapacity={} " +
                                "utilization={} message=Thread pool queue is nearly full — risk of task rejection",
                        queueSize, queueCapacity, String.format("%.1f%%", utilization * 100));
            } else if (utilization > 0.7) {
                log.warn("event=ALERT_WARN alert=THREAD_POOL_HIGH queueUsed={} queueCapacity={} " +
                                "utilization={} message=Thread pool queue usage is high",
                        queueSize, queueCapacity, String.format("%.1f%%", utilization * 100));
            }
        }
    }
}
