package com.systemforge.backend.system.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.systemforge.backend.architect.maba.MabaContext;
import com.systemforge.backend.architect.maba.MabaOrchestrator;
import com.systemforge.backend.common.enums.JobStatus;
import com.systemforge.backend.common.metrics.GenerationMetrics;
import com.systemforge.backend.common.event.EventBus;
import com.systemforge.backend.system.dto.GenerationProgressEvent;
import com.systemforge.backend.system.entity.GenerationJob;
import com.systemforge.backend.system.entity.UserSystemConfig;
import com.systemforge.backend.system.event.GenerationJobSubmittedEvent;
import com.systemforge.backend.system.repository.GenerationJobRepository;
import com.systemforge.backend.system.repository.UserSystemConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.annotation.PostConstruct;

/**
 * Background worker for executing AI generation jobs asynchronously.
 *
 * <p>Uses @TransactionalEventListener to ensure execution only begins
 * AFTER the PENDING job has been successfully committed to the database,
 * preventing race conditions.
 *
 * <p>Enterprise reliability features:
 * <ul>
 *   <li>Atomic PENDING→PROCESSING transition prevents duplicate execution</li>
 *   <li>Exponential backoff between retries (5s, 15s, 45s)</li>
 *   <li>Jackson-based MABA metadata serialization (no manual JSON)</li>
 *   <li>MDC-based structured logging for all downstream log lines</li>
 *   <li>Scheduled cleanup for stuck PROCESSING jobs</li>
 *   <li>Orphaned job cleanup on server startup</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GenerationWorker {

    /** Base delay for exponential retry backoff in milliseconds. */
    private static final long RETRY_BASE_DELAY_MS = 5_000; // 5 seconds

    /** Multiplier for exponential backoff. Delays: 5s, 15s, 45s. */
    private static final double RETRY_BACKOFF_MULTIPLIER = 3.0;

    /** Maximum time a job can stay in PROCESSING before being considered stuck. */
    private static final int STUCK_JOB_TIMEOUT_MINUTES = 10;

    private final GenerationJobRepository jobRepository;
    private final UserSystemConfigRepository configRepository;
    private final EventBus eventBus;
    private final MabaOrchestrator mabaOrchestrator;
    private final GenerationMetrics metrics;
    private final ObjectMapper objectMapper;

    /**
     * On server startup, fail ALL orphaned PENDING/PROCESSING jobs.
     * Their worker threads died when the previous JVM shut down — they will never complete.
     * This ensures no zombie jobs block future generation attempts for the same config.
     */
    @PostConstruct
    public void failOrphanedJobsOnStartup() {
        int failed = jobRepository.failAllOrphanedJobs(
                List.of(JobStatus.PENDING, JobStatus.PROCESSING),
                JobStatus.FAILED,
                "Server restarted — job worker was lost. Please retry.",
                LocalDateTime.now());
        if (failed > 0) {
            log.warn("event=STARTUP_ORPHAN_CLEANUP failedJobs={} message=Marked orphaned PENDING/PROCESSING jobs as FAILED", failed);
        } else {
            log.info("event=STARTUP_ORPHAN_CLEANUP failedJobs=0 message=No orphaned jobs found");
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("aiGenerationExecutor")
    public void handleGenerationJobSubmitted(GenerationJobSubmittedEvent event) {
        UUID jobId = event.jobId();

        GenerationJob job = jobRepository.findById(jobId).orElse(null);
        if (job == null) {
            log.error("event=JOB_NOT_FOUND jobId={} message=Job not found after commit, possible data inconsistency", jobId);
            return;
        }

        // Set MDC for all downstream log lines in this thread
        setJobMdc(job);
        try {
            log.info("event=JOB_PICKED_UP jobId={} userId={} status=PENDING", jobId, job.getUserId());
            executeWithRetries(job);
        } finally {
            MDC.remove("jobId");
            MDC.remove("userId");
        }
    }

    private void executeWithRetries(GenerationJob job) {
        UUID jobId = job.getId();

        // Transition to PROCESSING with atomic check-and-set
        LocalDateTime now = LocalDateTime.now();
        int updated = jobRepository.updateStatusConditionally(jobId, JobStatus.PROCESSING, JobStatus.PENDING, now);
        if (updated == 0) {
            log.warn("event=JOB_SKIP jobId={} userId={} message=Job is not PENDING, another worker may have picked it up",
                    jobId, job.getUserId());
            return;
        }

        job.setStatus(JobStatus.PROCESSING);
        job.setStartedAt(now);
        metrics.markProcessing();
        log.info("event=JOB_STARTED jobId={} userId={} status=PROCESSING attempt={}/{}",
                jobId, job.getUserId(), job.getRetryCount() + 1, job.getMaxRetries());

        MabaContext mabaContext = null;

        try {
            // ─── Step 1: Validate & Load Config ───────────────────────────
            eventBus.publish(jobId, GenerationProgressEvent.stepStarted("Config Validation", 1, 8));
            long stepStart = System.currentTimeMillis();

            UserSystemConfig config = configRepository.findById(job.getConfigId())
                    .orElseThrow(() -> new RuntimeException("Config not found: " + job.getConfigId()));

            long stepDuration = System.currentTimeMillis() - stepStart;
            eventBus.publish(jobId, GenerationProgressEvent.stepCompleted(
                    "Config Validation", 1, 8, "Config loaded and validated", stepDuration));
            eventBus.publish(jobId, GenerationProgressEvent.progress(5));
            log.debug("event=STEP_COMPLETED jobId={} step=ConfigValidation durationMs={}", jobId, stepDuration);

            // ─── Step 2: Execute MABA Pipeline ────────────────────────────
            String userRequirements = buildRequirementsFromConfig(config);

            mabaContext = new MabaContext(userRequirements);
            mabaContext.setUserId(job.getUserId());
            mabaContext.setJobId(jobId);

            log.info("event=MABA_PIPELINE_START jobId={} userId={}", jobId, job.getUserId());
            mabaOrchestrator.execute(mabaContext);

            if ("FAILED".equals(mabaContext.getStatus())) {
                throw new RuntimeException("MABA pipeline failed: " + mabaContext.getFailureReason());
            }

            String outputJson = mabaContext.getFinalDocument();

            // ─── Step 3: Persist Results ──────────────────────────────────
            eventBus.publish(jobId, GenerationProgressEvent.stepStarted("Persisting Results", 8, 8));
            stepStart = System.currentTimeMillis();

            config.setGeneratedOutputJson(outputJson);
            config.setGenerated(true);
            configRepository.saveAndFlush(config);

            job.setStatus(JobStatus.COMPLETED);
            job.setResultJson(outputJson);
            job.setMabaMetadata(serializeMetadata(mabaContext));
            job.setCompletedAt(LocalDateTime.now());
            jobRepository.saveAndFlush(job);

            stepDuration = System.currentTimeMillis() - stepStart;
            eventBus.publish(jobId, GenerationProgressEvent.stepCompleted(
                    "Persisting Results", 8, 8, "Results saved", stepDuration));

            // ─── Final: Completed ─────────────────────────────────────────
            eventBus.publish(jobId, GenerationProgressEvent.completed(jobId.toString()));
            eventBus.complete(jobId);

            long totalDurationMs = Duration.between(job.getStartedAt(), job.getCompletedAt()).toMillis();
            long totalTokens = mabaContext.getTotalPromptTokens() + mabaContext.getTotalCompletionTokens();

            metrics.incrementCompleted();
            metrics.getJobDuration().record(Duration.ofMillis(totalDurationMs));

            log.info("event=JOB_COMPLETED jobId={} userId={} status=COMPLETED durationMs={} totalTokens={} " +
                            "successfulAgents={} failedAgents={} warnings={} attempt={}/{}",
                    jobId, job.getUserId(), totalDurationMs, totalTokens,
                    mabaContext.getSuccessfulAgentCount(), mabaContext.getFailedAgentCount(),
                    mabaContext.getWarnings().size(),
                    job.getRetryCount() + 1, job.getMaxRetries());

        } catch (Exception e) {
            log.error("event=JOB_EXECUTION_ERROR jobId={} userId={} errorType={} error={}",
                    jobId, job.getUserId(), e.getClass().getSimpleName(), e.getMessage(), e);

            job.setRetryCount(job.getRetryCount() + 1);

            if (job.getRetryCount() >= job.getMaxRetries()) {
                // ─── Final failure: max retries exhausted ─────────────────
                job.setStatus(JobStatus.FAILED);
                job.setErrorMessage(truncateErrorMessage(
                        "Max retries exceeded. Last error: " + e.getMessage()));
                if (mabaContext != null) {
                    job.setMabaMetadata(serializeMetadata(mabaContext));
                }
                job.setCompletedAt(LocalDateTime.now());
                jobRepository.saveAndFlush(job);

                metrics.incrementFailed();

                // Notify SSE listener of failure
                eventBus.publish(jobId, GenerationProgressEvent.failed(jobId.toString(), job.getErrorMessage()));
                eventBus.complete(jobId);

                log.error("event=JOB_FAILED jobId={} userId={} status=FAILED retryCount={} maxRetries={} lastError={}",
                        jobId, job.getUserId(), job.getRetryCount(), job.getMaxRetries(), e.getMessage());
            } else {
                // ─── Retry with exponential backoff ───────────────────────
                long delayMs = calculateRetryDelay(job.getRetryCount());

                log.warn("event=JOB_RETRY jobId={} userId={} attempt={}/{} retryDelayMs={} error={}",
                        jobId, job.getUserId(), job.getRetryCount(), job.getMaxRetries(),
                        delayMs, e.getMessage());

                metrics.incrementRetries();

                job.setStatus(JobStatus.PENDING);
                job.setErrorMessage(truncateErrorMessage(
                        "Failed attempt " + job.getRetryCount() + ": " + e.getMessage()));
                jobRepository.saveAndFlush(job);

                // Backoff before retrying
                sleepBeforeRetry(delayMs);

                // Retry (recursive call re-checks PENDING atomically)
                executeWithRetries(job);
            }
        }
    }

    private String buildRequirementsFromConfig(UserSystemConfig config) {
        StringBuilder sb = new StringBuilder();
        sb.append("Design a backend system with the following specifications:\n\n");
        sb.append("Application Name: ").append(config.getConfigName()).append("\n");
        sb.append("Application Type: ").append(config.getAppType()).append("\n");
        sb.append("Application Scale: ").append(config.getAppScale()).append("\n");

        if (config.getSelectedSystemsJson() != null && !config.getSelectedSystemsJson().isBlank()) {
            sb.append("\nSelected Systems/Features:\n").append(config.getSelectedSystemsJson()).append("\n");
        }

        return sb.toString();
    }

    /**
     * Periodically cleans up jobs that have been stuck in PROCESSING state for too long.
     * Prevents the system from having orphaned jobs indefinitely.
     */
    @Scheduled(fixedRate = 300000) // Run every 5 minutes
    @Transactional
    public void cleanStuckJobs() {
        LocalDateTime timeoutThreshold = LocalDateTime.now().minusMinutes(STUCK_JOB_TIMEOUT_MINUTES);
        List<GenerationJob> stuckJobs = jobRepository.findByStatusAndStartedAtBefore(JobStatus.PROCESSING, timeoutThreshold);

        if (!stuckJobs.isEmpty()) {
            log.warn("event=STUCK_JOBS_DETECTED count={}", stuckJobs.size());
            for (GenerationJob job : stuckJobs) {
                job.setStatus(JobStatus.FAILED);
                job.setErrorMessage("Job exceeded maximum execution time limit (" + STUCK_JOB_TIMEOUT_MINUTES + " minutes).");
                job.setCompletedAt(LocalDateTime.now());
                jobRepository.save(job);

                metrics.incrementFailed();

                eventBus.publish(job.getId(), GenerationProgressEvent.failed(job.getId().toString(), job.getErrorMessage()));
                eventBus.complete(job.getId());

                log.warn("event=STUCK_JOB_FAILED jobId={} userId={} message=Timed out after {} minutes",
                        job.getId(), job.getUserId(), STUCK_JOB_TIMEOUT_MINUTES);
            }
        }
    }

    // ─── Utility ──────────────────────────────────────────────────────────

    /**
     * Serialize MABA metadata using Jackson instead of manual JSON.
     * Falls back to a minimal JSON string if serialization fails.
     */
    private String serializeMetadata(MabaContext mabaContext) {
        try {
            return objectMapper.writeValueAsString(mabaContext.toMetadataMap());
        } catch (Exception e) {
            log.error("event=METADATA_SERIALIZATION_FAILED traceId={} error={}",
                    mabaContext.getTraceId(), e.getMessage());
            return "{\"traceId\":\"" + mabaContext.getTraceId()
                    + "\",\"status\":\"" + mabaContext.getStatus()
                    + "\",\"serializationError\":\"" + e.getMessage().replace("\"", "'") + "\"}";
        }
    }

    /**
     * Calculate retry delay with exponential backoff.
     * Attempt 1: 5s, Attempt 2: 15s, Attempt 3: 45s.
     */
    private long calculateRetryDelay(int attemptNumber) {
        return (long) (RETRY_BASE_DELAY_MS * Math.pow(RETRY_BACKOFF_MULTIPLIER, attemptNumber - 1));
    }

    /**
     * Sleep with interruption check for retry backoff.
     */
    private void sleepBeforeRetry(long delayMs) {
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("event=RETRY_SLEEP_INTERRUPTED message=Retry delay was interrupted");
        }
    }

    /**
     * Truncate error messages to prevent blowing up the TEXT column.
     */
    private String truncateErrorMessage(String message) {
        if (message == null) return null;
        return message.length() > 2000 ? message.substring(0, 2000) + "..." : message;
    }

    private void setJobMdc(GenerationJob job) {
        MDC.put("jobId", job.getId().toString());
        MDC.put("userId", job.getUserId().toString());
    }
}
