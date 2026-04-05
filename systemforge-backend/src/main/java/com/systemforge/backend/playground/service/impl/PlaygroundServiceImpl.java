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
        long startTime = System.currentTimeMillis();
        PlaygroundGeneratedOutput output = engine.generate(request);
        long duration = System.currentTimeMillis() - startTime;
        
        log.info("[PLAYGROUND_GENERATE] type={}, variant={}, features={}, timeMs={}",
                request.getServiceType(), request.getVariant(), request.getFeatures(), duration);

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

    @Override
    @Transactional
    public byte[] exportZip(PlaygroundConfigRequest request) {
        long startTime = System.currentTimeMillis();
        // Generate (or fetch from cache) the configuration
        PlaygroundGeneratedOutput output = this.generate(request);
        
        try (java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
             java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(baos)) {
             
            String basePackage = "src/main/java/com/systemforge/generated/";
            
            // Add Controller
            addFileToZip(zos, basePackage + "controller/Controller.java", 
                output.getPreview().getGeneratedCode().getControllerCode());
                
            // Add Service
            addFileToZip(zos, basePackage + "service/Service.java", 
                output.getPreview().getGeneratedCode().getServiceCode());
                
            // Add Config
            addFileToZip(zos, basePackage + "config/Config.java", 
                output.getPreview().getGeneratedCode().getConfigCode());
                
            // Add Security
            addFileToZip(zos, basePackage + "security/SecurityConfig.java", 
                output.getPreview().getGeneratedCode().getSecurityCode());
                
            // Add a basic pom.xml
            addFileToZip(zos, "pom.xml", generateBasicPomXml(output));

            zos.finish();
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("[PLAYGROUND_EXPORT] type={}, variant={}, features={}, timeMs={}, sizeBytes={}",
                request.getServiceType(), request.getVariant(), request.getFeatures(), duration, baos.size());
                
            return baos.toByteArray();
        } catch (java.io.IOException e) {
            log.error("Failed to generate ZIP export", e);
            throw new com.systemforge.backend.common.exception.BusinessException(
                "PLAYGROUND_ZIP_ERROR", "Failed to generate ZIP file", org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ─── Private helpers ───────────────────────────────────────────────────────
    
    private void addFileToZip(java.util.zip.ZipOutputStream zos, String path, String content) throws java.io.IOException {
        if (content == null || content.isBlank()) return;
        
        // Add package declaration to the top if it's a java file
        if (path.endsWith(".java")) {
            String packageName = path.replace("src/main/java/", "").replace("/", ".").replace(".java", "");
            packageName = packageName.substring(0, packageName.lastIndexOf('.'));
            content = "package " + packageName + ";\n\n" + content;
        }
        
        java.util.zip.ZipEntry entry = new java.util.zip.ZipEntry(path);
        zos.putNextEntry(entry);
        zos.write(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        zos.closeEntry();
    }
    
    private String generateBasicPomXml(PlaygroundGeneratedOutput output) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
               "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
               "\txsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" +
               "\t<modelVersion>4.0.0</modelVersion>\n" +
               "\t<parent>\n" +
               "\t\t<groupId>org.springframework.boot</groupId>\n" +
               "\t\t<artifactId>spring-boot-starter-parent</artifactId>\n" +
               "\t\t<version>3.4.3</version>\n" +
               "\t\t<relativePath/> <!-- lookup parent from repository -->\n" +
               "\t</parent>\n" +
               "\t<groupId>com.systemforge</groupId>\n" +
               "\t<artifactId>generated-microservice</artifactId>\n" +
               "\t<version>0.0.1-SNAPSHOT</version>\n" +
               "\t<name>generated-microservice</name>\n" +
               "\t<description>Generated by SystemForge Microservices Playground</description>\n" +
               "\t<properties>\n" +
               "\t\t<java.version>21</java.version>\n" +
               "\t</properties>\n" +
               "\t<dependencies>\n" +
               "\t\t<dependency>\n" +
               "\t\t\t<groupId>org.springframework.boot</groupId>\n" +
               "\t\t\t<artifactId>spring-boot-starter-web</artifactId>\n" +
               "\t\t</dependency>\n" +
               "\t\t<dependency>\n" +
               "\t\t\t<groupId>org.springframework.boot</groupId>\n" +
               "\t\t\t<artifactId>spring-boot-starter-security</artifactId>\n" +
               "\t\t</dependency>\n" +
               "\t</dependencies>\n" +
               "</project>";
    }

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
            PlaygroundGeneratedOutput output = objectMapper.readValue(config.getGeneratedOutputJson(), PlaygroundGeneratedOutput.class);
            output.setId(config.getId() != null ? config.getId().toString() : UUID.randomUUID().toString());
            output.setCreatedAt(config.getCreatedAt());
            return Optional.of(output);
        } catch (JsonProcessingException e) {
            log.warn("Failed to deserialize playground output for config: {}", config.getId());
            return Optional.empty();
        }
    }
}
