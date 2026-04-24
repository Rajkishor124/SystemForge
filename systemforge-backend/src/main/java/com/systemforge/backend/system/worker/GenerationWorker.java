package com.systemforge.backend.system.worker;

import com.systemforge.backend.architect.maba.MabaContext;
import com.systemforge.backend.architect.maba.MabaOrchestrator;
import com.systemforge.backend.common.enums.JobStatus;
import com.systemforge.backend.common.sse.SseEmitterRegistry;
import com.systemforge.backend.system.dto.GenerationProgressEvent;
import com.systemforge.backend.system.entity.GenerationJob;
import com.systemforge.backend.system.entity.UserSystemConfig;
import com.systemforge.backend.system.event.GenerationJobSubmittedEvent;
import com.systemforge.backend.system.repository.GenerationJobRepository;
import com.systemforge.backend.system.repository.UserSystemConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Background worker for executing AI generation jobs asynchronously.
 * 
 * <p>Uses @TransactionalEventListener to ensure execution only begins
 * AFTER the PENDING job has been successfully committed to the database,
 * preventing race conditions.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GenerationWorker {

    private final GenerationJobRepository jobRepository;
    private final UserSystemConfigRepository configRepository;
    private final SseEmitterRegistry sseRegistry;
    private final MabaOrchestrator mabaOrchestrator;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("aiGenerationExecutor")
    public void handleGenerationJobSubmitted(GenerationJobSubmittedEvent event) {
        UUID jobId = event.jobId();
        log.info("[ASYNC] Handling submitted generation job: {}", jobId);

        GenerationJob job = jobRepository.findById(jobId).orElse(null);
        if (job == null) {
            log.error("[ASYNC] Job not found: {}", jobId);
            return;
        }

        executeWithRetries(job);
    }

    private void executeWithRetries(GenerationJob job) {
        UUID jobId = job.getId();
        
        // Transition to PROCESSING
        job.setStatus(JobStatus.PROCESSING);
        job.setStartedAt(LocalDateTime.now());
        jobRepository.save(job);

        MabaContext mabaContext = null;

        try {
            // ─── Step 1: Validate & Load Config ───────────────────────────
            sseRegistry.send(jobId, GenerationProgressEvent.stepStarted("Config Validation", 1, 8));
            long stepStart = System.currentTimeMillis();

            UserSystemConfig config = configRepository.findById(job.getConfigId())
                    .orElseThrow(() -> new RuntimeException("Config not found: " + job.getConfigId()));

            long stepDuration = System.currentTimeMillis() - stepStart;
            sseRegistry.send(jobId, GenerationProgressEvent.stepCompleted(
                    "Config Validation", 1, 8, "Config loaded and validated", stepDuration));
            sseRegistry.send(jobId, GenerationProgressEvent.progress(5));

            // ─── Step 2: Execute MABA Pipeline ────────────────────────────
            String userRequirements = buildRequirementsFromConfig(config);

            mabaContext = new MabaContext(userRequirements);
            mabaContext.setUserId(job.getUserId());
            mabaContext.setJobId(jobId);

            mabaOrchestrator.execute(mabaContext);

            if ("FAILED".equals(mabaContext.getStatus())) {
                throw new RuntimeException("MABA pipeline failed: " + mabaContext.getFailureReason());
            }

            String outputJson = mabaContext.getFinalDocument();

            // ─── Step 3: Persist Results ──────────────────────────────────
            sseRegistry.send(jobId, GenerationProgressEvent.stepStarted("Persisting Results", 8, 8));
            stepStart = System.currentTimeMillis();

            config.setGeneratedOutputJson(outputJson);
            config.setGenerated(true);
            configRepository.save(config);

            job.setStatus(JobStatus.COMPLETED);
            job.setResultJson(outputJson);
            job.setMabaMetadata(mabaContext.toMetadataJson());
            job.setCompletedAt(LocalDateTime.now());
            jobRepository.save(job);

            stepDuration = System.currentTimeMillis() - stepStart;
            sseRegistry.send(jobId, GenerationProgressEvent.stepCompleted(
                    "Persisting Results", 8, 8, "Results saved", stepDuration));

            // ─── Final: Completed ─────────────────────────────────────────
            sseRegistry.send(jobId, GenerationProgressEvent.completed(jobId.toString()));
            sseRegistry.complete(jobId);

            log.info("[ASYNC] MABA generation completed: jobId={}, durationMs={}, totalTokens={}",
                    jobId,
                    java.time.Duration.between(job.getStartedAt(), job.getCompletedAt()).toMillis(),
                    mabaContext.getTotalPromptTokens() + mabaContext.getTotalCompletionTokens());

        } catch (Exception e) {
            log.error("[ASYNC] Generation failed for jobId={}: {}", jobId, e.getMessage(), e);

            job.setRetryCount(job.getRetryCount() + 1);
            
            if (job.getRetryCount() >= job.getMaxRetries()) {
                job.setStatus(JobStatus.FAILED);
                job.setErrorMessage("Max retries exceeded. Last error: " + e.getMessage());
                if (mabaContext != null) {
                    job.setMabaMetadata(mabaContext.toMetadataJson());
                }
                job.setCompletedAt(LocalDateTime.now());
                jobRepository.save(job);

                // Notify SSE listener of failure
                sseRegistry.send(jobId, GenerationProgressEvent.failed(jobId.toString(), job.getErrorMessage()));
                sseRegistry.complete(jobId);
            } else {
                log.warn("[ASYNC] Retrying job {}. Attempt {}/{}", jobId, job.getRetryCount(), job.getMaxRetries());
                job.setStatus(JobStatus.PENDING);
                job.setErrorMessage("Failed attempt " + job.getRetryCount() + ": " + e.getMessage());
                jobRepository.save(job);
                
                // Retry immediately (for a real system, you might want an exponential backoff via ScheduledExecutor)
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
        LocalDateTime timeoutThreshold = LocalDateTime.now().minusMinutes(10); // 10 minutes timeout
        List<GenerationJob> stuckJobs = jobRepository.findByStatusAndStartedAtBefore(JobStatus.PROCESSING, timeoutThreshold);
        
        if (!stuckJobs.isEmpty()) {
            log.warn("[ASYNC] Found {} stuck jobs. Marking them as FAILED.", stuckJobs.size());
            for (GenerationJob job : stuckJobs) {
                job.setStatus(JobStatus.FAILED);
                job.setErrorMessage("Job exceeded maximum execution time limit.");
                job.setCompletedAt(LocalDateTime.now());
                jobRepository.save(job);
                
                sseRegistry.send(job.getId(), GenerationProgressEvent.failed(job.getId().toString(), job.getErrorMessage()));
                sseRegistry.complete(job.getId());
            }
        }
    }
}
