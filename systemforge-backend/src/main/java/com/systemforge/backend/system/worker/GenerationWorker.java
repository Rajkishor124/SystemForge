package com.systemforge.backend.system.worker;

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

/**
 * Background worker for executing AI generation jobs asynchronously.
 *
 * <p>Uses @TransactionalEventListener to ensure execution only begins
 * AFTER the PENDING job has been successfully committed to the database,
 * preventing race conditions.
 *
 * <p>Structured logging: every log line includes jobId, userId, status
 * via MDC for correlation in ELK/Datadog.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GenerationWorker {

    private final GenerationJobRepository jobRepository;
    private final UserSystemConfigRepository configRepository;
    private final EventBus eventBus;
    private final MabaOrchestrator mabaOrchestrator;
    private final GenerationMetrics metrics;

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
            job.setMabaMetadata(mabaContext.toMetadataJson());
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

            log.info("event=JOB_COMPLETED jobId={} userId={} status=COMPLETED durationMs={} totalTokens={} attempt={}/{}",
                    jobId, job.getUserId(), totalDurationMs, totalTokens,
                    job.getRetryCount() + 1, job.getMaxRetries());

        } catch (Exception e) {
            log.error("event=JOB_EXECUTION_ERROR jobId={} userId={} error={}", jobId, job.getUserId(), e.getMessage(), e);

            job.setRetryCount(job.getRetryCount() + 1);

            if (job.getRetryCount() >= job.getMaxRetries()) {
                job.setStatus(JobStatus.FAILED);
                job.setErrorMessage("Max retries exceeded. Last error: " + e.getMessage());
                if (mabaContext != null) {
                    job.setMabaMetadata(mabaContext.toMetadataJson());
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
                log.warn("event=JOB_RETRY jobId={} userId={} attempt={}/{} error={}",
                        jobId, job.getUserId(), job.getRetryCount(), job.getMaxRetries(), e.getMessage());

                metrics.incrementRetries();

                job.setStatus(JobStatus.PENDING);
                job.setErrorMessage("Failed attempt " + job.getRetryCount() + ": " + e.getMessage());
                jobRepository.saveAndFlush(job);

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
        LocalDateTime timeoutThreshold = LocalDateTime.now().minusMinutes(10);
        List<GenerationJob> stuckJobs = jobRepository.findByStatusAndStartedAtBefore(JobStatus.PROCESSING, timeoutThreshold);

        if (!stuckJobs.isEmpty()) {
            log.warn("event=STUCK_JOBS_DETECTED count={}", stuckJobs.size());
            for (GenerationJob job : stuckJobs) {
                job.setStatus(JobStatus.FAILED);
                job.setErrorMessage("Job exceeded maximum execution time limit (10 minutes).");
                job.setCompletedAt(LocalDateTime.now());
                jobRepository.save(job);

                metrics.incrementFailed();

                eventBus.publish(job.getId(), GenerationProgressEvent.failed(job.getId().toString(), job.getErrorMessage()));
                eventBus.complete(job.getId());

                log.warn("event=STUCK_JOB_FAILED jobId={} userId={} message=Timed out after 10 minutes",
                        job.getId(), job.getUserId());
            }
        }
    }

    private void setJobMdc(GenerationJob job) {
        MDC.put("jobId", job.getId().toString());
        MDC.put("userId", job.getUserId().toString());
    }
}
