package com.systemforge.backend.system.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.systemforge.backend.common.dto.PagedResponse;
import com.systemforge.backend.common.enums.JobStatus;
import com.systemforge.backend.common.enums.JobType;
import com.systemforge.backend.common.enums.SystemType;
import com.systemforge.backend.common.exception.BusinessException;
import com.systemforge.backend.common.exception.ResourceNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import com.systemforge.backend.common.security.InputSanitizer;
import com.systemforge.backend.common.sse.SseEmitterRegistry;
import com.systemforge.backend.architect.maba.MabaContext;
import com.systemforge.backend.architect.maba.MabaOrchestrator;
import com.systemforge.backend.recommendation.dto.RecommendationRequest;
import com.systemforge.backend.recommendation.dto.RecommendationResult;
import com.systemforge.backend.recommendation.service.RecommendationService;
import com.systemforge.backend.system.dto.GenerationJobDto;
import com.systemforge.backend.system.dto.GenerationProgressEvent;
import com.systemforge.backend.system.dto.SystemDefinitionDto;
import com.systemforge.backend.system.dto.UserSystemConfigDto;
import com.systemforge.backend.system.dto.request.CreateSystemConfigRequest;
import com.systemforge.backend.system.entity.GenerationJob;
import com.systemforge.backend.system.entity.UserSystemConfig;
import com.systemforge.backend.system.mapper.SystemMapper;
import com.systemforge.backend.system.repository.GenerationJobRepository;
import com.systemforge.backend.system.repository.SystemDefinitionRepository;
import com.systemforge.backend.system.repository.UserSystemConfigRepository;
import com.systemforge.backend.system.service.SystemService;
import com.systemforge.backend.system.event.GenerationJobSubmittedEvent;
import org.springframework.context.ApplicationEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.context.ApplicationContext;
import org.springframework.beans.factory.annotation.Qualifier;
import java.util.concurrent.Executor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
@Service
@RequiredArgsConstructor
@Slf4j
public class SystemServiceImpl implements SystemService {

    private final SystemDefinitionRepository systemDefinitionRepository;
    private final UserSystemConfigRepository configRepository;
    private final GenerationJobRepository jobRepository;
    private final SystemMapper systemMapper;
    private final RecommendationService recommendationService;
    private final ObjectMapper objectMapper;
    private final SseEmitterRegistry sseRegistry;
    private final MabaOrchestrator mabaOrchestrator;
    private final ApplicationContext applicationContext;
    private final ApplicationEventPublisher eventPublisher;
    private final @Qualifier("aiGenerationExecutor") Executor aiExecutor;

    /**
     * Returns the Spring AOP proxy of this bean.
     * Required because @Async on self-invoked methods is silently ignored.
     */
    private SystemServiceImpl self() {
        return applicationContext.getBean(SystemServiceImpl.class);
    }

    // ─── System Catalog ───────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "systemDefinitions")
    public List<SystemDefinitionDto> getAllSystems() {
        return systemDefinitionRepository.findByIsActiveTrue().stream()
                .map(systemMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SystemDefinitionDto> getSystemsByType(SystemType type) {
        return systemDefinitionRepository.findBySystemTypeAndIsActiveTrue(type).stream()
                .map(systemMapper::toDto)
                .toList();
    }

    // ─── User Config Management ───────────────────────────────────────────

    @Override
    @Transactional
    public UserSystemConfigDto createConfig(UUID userId, CreateSystemConfigRequest request) {
        log.info("Creating new system config for user: {}", userId);
        
        UserSystemConfig config = UserSystemConfig.builder()
                .userId(userId)
                .configName(InputSanitizer.sanitize(request.getConfigName()))
                .appType(request.getAppType())
                .appScale(request.getAppScale())
                .selectedSystemsJson(InputSanitizer.sanitizeMultiline(request.getSelectedSystemsJson()))
                .build();
                
        // builder.default is not always respected perfectly by mapstruct, but for entity builder it works ok,
        // let's explicitly set generated false just in case
        config.setGenerated(false);
                
        UserSystemConfig saved = configRepository.save(config);
        return systemMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public UserSystemConfigDto getConfigById(UUID userId, UUID configId) {
        UserSystemConfig config = configRepository.findByIdAndUserId(configId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("SYS_001", "Config not found with id: " + configId));
        return systemMapper.toDto(config);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<UserSystemConfigDto> getUserConfigs(UUID userId, Pageable pageable) {
        Page<UserSystemConfig> page = configRepository.findByUserId(userId, pageable);
        return PagedResponse.from(page, systemMapper::toDto);
    }

    @Override
    @Transactional
    public UserSystemConfigDto generateArchitecture(UUID userId, UUID configId) {
        log.info("Triggering synchronous architecture generation for config: {}, user: {}", configId, userId);
        
        UserSystemConfig config = configRepository.findByIdAndUserId(configId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("SYS_001", "Config not found with id: " + configId));
                
        if (config.isGenerated()) {
            throw new BusinessException("SYS_002", "Architecture already generated for this config");
        }
        
        // Prepare request for AI Engine
        RecommendationRequest aiRequest = RecommendationRequest.builder()
                .appType(config.getAppType())
                .scale(config.getAppScale())
                .build();
                
        RecommendationResult result = recommendationService.recommend(aiRequest);
        
        try {
            String outputJson = objectMapper.writeValueAsString(result);
            config.setGeneratedOutputJson(outputJson);
            config.setGenerated(true);
            
            UserSystemConfig saved = configRepository.save(config);
            return systemMapper.toDto(saved);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize AI recommendation result", e);
            throw new BusinessException("SYS_003", "Failed to process architecture generation output");
        }
    }

    @Override
    @Transactional
    public void deleteConfig(UUID userId, UUID configId) {
        log.info("Deleting config: {} for user: {}", configId, userId);
        UserSystemConfig config = configRepository.findByIdAndUserId(configId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("SYS_001", "Config not found with id: " + configId));
                
        config.setDeleted(true);
        configRepository.save(config);
    }

    // ─── Async Generation ─────────────────────────────────────────────────

    @Override
    @Transactional
    public GenerationJobDto submitGeneration(UUID userId, UUID configId) {
        log.info("Submitting async generation job for config: {}, user: {}", configId, userId);

        // Validate config exists and belongs to user
        UserSystemConfig config = configRepository.findByIdAndUserId(configId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("SYS_001", "Config not found with id: " + configId));

        if (config.isGenerated()) {
            throw new BusinessException("SYS_002", "Architecture already generated for this config",
                    HttpStatus.CONFLICT);
        }

        // Check system backpressure (Executor Overload)
        if (aiExecutor instanceof ThreadPoolTaskExecutor taskExecutor) {
            if (taskExecutor.getThreadPoolExecutor().getQueue().remainingCapacity() == 0) {
                log.warn("System overloaded: AI Generation queue is full");
                throw new BusinessException("SYS_004", "System is busy, please try again", HttpStatus.SERVICE_UNAVAILABLE);
            }
        }

        // Create a PENDING job
        GenerationJob job = GenerationJob.builder()
                .userId(userId)
                .configId(configId)
                .jobType(JobType.SYSTEM_GENERATION)
                .status(JobStatus.PENDING)
                .build();

        GenerationJob saved = jobRepository.save(job);
        log.info("Generation job created: jobId={}", saved.getId());

        // Publish event to trigger async worker AFTER commit
        eventPublisher.publishEvent(new GenerationJobSubmittedEvent(saved.getId()));

        return toJobDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public GenerationJobDto getJobStatus(UUID userId, UUID jobId) {
        GenerationJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("JOB_001", "Job not found with id: " + jobId));
                
        if (!job.getUserId().equals(userId)) {
            throw new AccessDeniedException("You do not have permission to access this job's stream.");
        }
        
        return toJobDto(job);
    }

    // ─── Mapping ──────────────────────────────────────────────────────────

    private GenerationJobDto toJobDto(GenerationJob job) {
        return GenerationJobDto.builder()
                .id(job.getId())
                .configId(job.getConfigId())
                .sessionId(job.getSessionId())
                .jobType(job.getJobType())
                .status(job.getStatus())
                .resultJson(job.getResultJson())
                .errorMessage(job.getErrorMessage())
                .mabaMetadata(job.getMabaMetadata())
                .startedAt(job.getStartedAt())
                .completedAt(job.getCompletedAt())
                .createdAt(job.getCreatedAt())
                .build();
    }
}
