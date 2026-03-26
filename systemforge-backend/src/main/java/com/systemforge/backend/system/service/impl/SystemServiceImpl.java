package com.systemforge.backend.system.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.systemforge.backend.common.dto.PagedResponse;
import com.systemforge.backend.common.enums.SystemType;
import com.systemforge.backend.common.exception.BusinessException;
import com.systemforge.backend.common.exception.ResourceNotFoundException;
import com.systemforge.backend.recommendation.dto.RecommendationRequest;
import com.systemforge.backend.recommendation.dto.RecommendationResult;
import com.systemforge.backend.recommendation.service.RecommendationService;
import com.systemforge.backend.system.dto.SystemDefinitionDto;
import com.systemforge.backend.system.dto.UserSystemConfigDto;
import com.systemforge.backend.system.dto.request.CreateSystemConfigRequest;
import com.systemforge.backend.system.entity.UserSystemConfig;
import com.systemforge.backend.system.mapper.SystemMapper;
import com.systemforge.backend.system.repository.SystemDefinitionRepository;
import com.systemforge.backend.system.repository.UserSystemConfigRepository;
import com.systemforge.backend.system.service.SystemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SystemServiceImpl implements SystemService {

    private final SystemDefinitionRepository systemDefinitionRepository;
    private final UserSystemConfigRepository configRepository;
    private final SystemMapper systemMapper;
    private final RecommendationService recommendationService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
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

    @Override
    @Transactional
    public UserSystemConfigDto createConfig(UUID userId, CreateSystemConfigRequest request) {
        log.info("Creating new system config for user: {}", userId);
        
        UserSystemConfig config = UserSystemConfig.builder()
                .userId(userId)
                .configName(request.getConfigName())
                .appType(request.getAppType())
                .appScale(request.getAppScale())
                .selectedSystemsJson(request.getSelectedSystemsJson())
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
        log.info("Triggering architecture generation for config: {}, user: {}", configId, userId);
        
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
}
