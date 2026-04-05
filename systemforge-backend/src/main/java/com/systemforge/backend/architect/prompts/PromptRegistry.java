package com.systemforge.backend.architect.prompts;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry that pre-loads all prompt templates from classpath on startup.
 *
 * <p>Templates are loaded from {@code classpath:prompts/architect/}
 * and keyed by filename (without extension).
 *
 * <p>Thread-safe, effectively immutable after {@link #init()}.
 */
@Component
@Slf4j
public class PromptRegistry {

    private static final String PROMPT_LOCATION = "classpath:prompts/architect/*.txt";

    private final Map<String, PromptTemplate> templates = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources(PROMPT_LOCATION);

            for (Resource resource : resources) {
                String filename = resource.getFilename();
                if (filename == null) continue;

                String name = filename.replace(".txt", "");
                String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

                templates.put(name, new PromptTemplate(name, content));
                log.info("[ARCHITECT_PROMPTS] Loaded prompt template: {}", name);
            }

            log.info("[ARCHITECT_PROMPTS] Total templates loaded: {}", templates.size());

        } catch (IOException e) {
            log.warn("[ARCHITECT_PROMPTS] Failed to load prompt templates: {}", e.getMessage());
        }
    }

    /**
     * Get a prompt template by name.
     *
     * @param name template name (filename without .txt)
     * @return the template
     * @throws IllegalArgumentException if template not found
     */
    public PromptTemplate get(String name) {
        PromptTemplate template = templates.get(name);
        if (template == null) {
            throw new IllegalArgumentException("Prompt template not found: " + name);
        }
        return template;
    }

    /**
     * Check if a template exists.
     */
    public boolean has(String name) {
        return templates.containsKey(name);
    }
}
