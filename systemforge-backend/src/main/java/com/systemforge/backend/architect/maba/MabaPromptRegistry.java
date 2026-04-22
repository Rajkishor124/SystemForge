package com.systemforge.backend.architect.maba;

import com.systemforge.backend.common.enums.AgentRole;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;

/**
 * Registry that loads the Multi-Agent Backend Architecture (MABA) prompts
 * from {@code classpath:prompts/maba/*.md} and maps them to {@link AgentRole} values.
 *
 * <p>Each prompt file is the full system-instruction for one specialist agent
 * (e.g., orchestrator.md → {@link AgentRole#ORCHESTRATOR}).
 *
 * <p>Thread-safe and effectively immutable after {@link #init()}.
 */
@Component
@Slf4j
public class MabaPromptRegistry {

    private static final String PROMPT_LOCATION = "classpath:prompts/maba/*.md";

    private final Map<AgentRole, String> prompts = new EnumMap<>(AgentRole.class);

    /** Maps filename (without extension) to AgentRole. */
    private static final Map<String, AgentRole> FILE_TO_ROLE = Map.of(
            "orchestrator", AgentRole.ORCHESTRATOR,
            "rag_engine", AgentRole.RAG_ENGINE,
            "requirements_analyst", AgentRole.REQUIREMENTS_ANALYST,
            "system_architect", AgentRole.SYSTEM_ARCHITECT,
            "db_designer", AgentRole.DB_DESIGNER,
            "api_designer", AgentRole.API_DESIGNER,
            "scalability_engineer", AgentRole.SCALABILITY_ENGINEER,
            "security_engineer", AgentRole.SECURITY_ENGINEER,
            "implementation_planner", AgentRole.IMPLEMENTATION_PLANNER,
            "final_synthesizer", AgentRole.FINAL_SYNTHESIZER
    );

    @PostConstruct
    public void init() {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources(PROMPT_LOCATION);

            for (Resource resource : resources) {
                String filename = resource.getFilename();
                if (filename == null) continue;

                String key = filename.replace(".md", "");
                AgentRole role = FILE_TO_ROLE.get(key);
                if (role == null) {
                    log.warn("[MABA_PROMPTS] Unknown prompt file (no AgentRole mapping): {}", filename);
                    continue;
                }

                String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                prompts.put(role, content);
                log.info("[MABA_PROMPTS] Loaded prompt for agent: {} ({} chars)", role.getRoleName(), content.length());
            }

            log.info("[MABA_PROMPTS] Total MABA prompts loaded: {}/{}", prompts.size(), AgentRole.values().length);

            // Warn about any missing prompts
            for (AgentRole role : AgentRole.values()) {
                if (!prompts.containsKey(role)) {
                    log.warn("[MABA_PROMPTS] Missing prompt for agent: {}", role.getRoleName());
                }
            }

        } catch (IOException e) {
            log.error("[MABA_PROMPTS] Failed to load MABA prompt templates: {}", e.getMessage());
        }
    }

    /**
     * Get the system prompt for a given agent role.
     *
     * @param role the agent role
     * @return the full system-instruction text
     * @throws IllegalArgumentException if no prompt is loaded for this role
     */
    public String getPrompt(AgentRole role) {
        String prompt = prompts.get(role);
        if (prompt == null) {
            throw new IllegalArgumentException("No MABA prompt loaded for role: " + role);
        }
        return prompt;
    }

    /**
     * Check if a prompt exists for the given role.
     */
    public boolean hasPrompt(AgentRole role) {
        return prompts.containsKey(role);
    }
}
