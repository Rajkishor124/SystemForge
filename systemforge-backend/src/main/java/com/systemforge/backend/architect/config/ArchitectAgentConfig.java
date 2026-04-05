package com.systemforge.backend.architect.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Getter;
import lombok.Setter;

/**
 * Configuration for the AI Architect Agent.
 */
@Configuration
@ConfigurationProperties(prefix = "architect")
@Getter
@Setter
public class ArchitectAgentConfig {

    /** Maximum conversation history messages to include in context. */
    private int maxHistoryMessages = 10;

    /** Maximum tool invocations per request (prevents loops). */
    private int maxToolInvocations = 5;

    /** LLM call timeout in seconds. */
    private int llmTimeoutSeconds = 60;

    /** Whether to persist reasoning steps to database. */
    private boolean persistSteps = true;
}
