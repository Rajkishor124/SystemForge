package com.systemforge.backend.playground.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.systemforge.backend.auth.util.SecurityPrincipalUtil;
import com.systemforge.backend.playground.cache.OutputCacheService;
import com.systemforge.backend.playground.dto.PlaygroundConfigRequest;
import com.systemforge.backend.playground.dto.PlaygroundGeneratedOutput;
import com.systemforge.backend.playground.engine.ServiceBuilderEngine;
import com.systemforge.backend.playground.entity.PlaygroundConfig;
import com.systemforge.backend.playground.enums.FeatureToggle;
import com.systemforge.backend.playground.enums.ServiceType;
import com.systemforge.backend.playground.enums.ServiceVariant;
import com.systemforge.backend.playground.repository.PlaygroundConfigRepository;
import com.systemforge.backend.playground.service.PlaygroundService;
import com.systemforge.backend.playground.validator.PlaygroundConfigValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Core service implementation.
 *
 * <p>Flow for generate():
 * <ol>
 *   <li>Validate config via PlaygroundConfigValidator</li>
 *   <li>Check cache (SHA-256 of full config JSON)</li>
 *   <li>If miss → delegate to ServiceBuilderEngine</li>
 *   <li>Cache result + persist to DB</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PlaygroundServiceImpl implements PlaygroundService {

    private final PlaygroundConfigValidator validator;
    private final ServiceBuilderEngine engine;
    private final OutputCacheService cacheService;
    private final PlaygroundConfigRepository repository;
    private final ObjectMapper objectMapper;

    @Override
    public List<ServiceType> getServiceTypes() {
        return Arrays.asList(ServiceType.values());
    }

    @Override
    public List<ServiceVariant> getVariants(ServiceType type) {
        return Arrays.stream(ServiceVariant.values())
                .filter(v -> v.belongsTo(type))
                .collect(Collectors.toList());
    }

    @Override
    public List<FeatureToggle> getFeatures(ServiceType type, ServiceVariant variant) {
        return Arrays.stream(FeatureToggle.values())
                .filter(f -> f.isCompatibleWith(type))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PlaygroundGeneratedOutput generate(PlaygroundConfigRequest request) {
        // ① Validate
        validator.validate(request);

        // ② Check cache
        String configJson = serializeConfig(request);
        String cacheKey = cacheService.buildKey(configJson);

        Optional<PlaygroundGeneratedOutput> cached = cacheService.get(cacheKey);
        if (cached.isPresent()) {
            log.info("Returning cached playground output for key: {}", cacheKey.substring(0, 12));
            return cached.get();
        }

        // ③ Generate via engine
        PlaygroundGeneratedOutput output = engine.generate(request);

        // ④ Cache result
        cacheService.put(cacheKey, output);

        // ⑤ Persist to DB
        persistConfig(request, output);

        return output;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlaygroundGeneratedOutput> getHistory() {
        UUID userId = SecurityPrincipalUtil.getAuthenticatedUserId();
        List<PlaygroundConfig> configs = repository.findByUserIdOrderByCreatedAtDesc(userId);

        return configs.stream()
                .map(this::deserializeOutput)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    // ─── Private helpers ───────────────────────────────────────────────────────

    private void persistConfig(PlaygroundConfigRequest request, PlaygroundGeneratedOutput output) {
        try {
            UUID userId = SecurityPrincipalUtil.getAuthenticatedUserId();

            String featuresJson = objectMapper.writeValueAsString(request.getFeatures());
            String outputJson = objectMapper.writeValueAsString(output);

            PlaygroundConfig config = PlaygroundConfig.builder()
                    .userId(userId)
                    .serviceType(request.getServiceType().name())
                    .variant(request.getVariant().name())
                    .featuresJson(featuresJson)
                    .generatedOutputJson(outputJson)
                    .build();

            repository.save(config);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize playground config for persistence", e);
        }
    }

    private String serializeConfig(PlaygroundConfigRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            // Fallback to a simple string representation
            return request.getServiceType() + "_" + request.getVariant() + "_" + request.getFeatures();
        }
    }

    private Optional<PlaygroundGeneratedOutput> deserializeOutput(PlaygroundConfig config) {
        try {
            if (config.getGeneratedOutputJson() == null) return Optional.empty();
            return Optional.of(
                    objectMapper.readValue(config.getGeneratedOutputJson(), PlaygroundGeneratedOutput.class)
            );
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize playground output for config: {}", config.getId());
            return Optional.empty();
        }
    }
}
