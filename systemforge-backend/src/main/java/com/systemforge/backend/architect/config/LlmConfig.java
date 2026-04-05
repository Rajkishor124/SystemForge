package com.systemforge.backend.architect.config;

import com.openai.client.OpenAIClient;
import com.systemforge.backend.architect.llm.LlmClient;
import com.systemforge.backend.architect.llm.impl.OpenAiLlmClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for LLM clients used by the AI Architect.
 */
@Configuration
public class LlmConfig {

    /**
     * Registers the OpenAI LLM client if the provider is set to openai (or missing).
     * <p>
     * This decouples the interface from the implementation, making it easy to swap
     * providers in the future (e.g., Claude, Local).
     */
    @Bean
    @ConditionalOnProperty(name = "llm.provider", havingValue = "openai", matchIfMissing = true)
    public LlmClient openAiClient(OpenAIClient openAIClient) {
        return new OpenAiLlmClient(openAIClient);
    }
}
