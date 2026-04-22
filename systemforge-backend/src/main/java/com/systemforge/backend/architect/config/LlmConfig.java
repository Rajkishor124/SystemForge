package com.systemforge.backend.architect.config;

import com.openai.client.OpenAIClient;
import com.systemforge.backend.architect.llm.LlmClient;
import com.systemforge.backend.architect.llm.impl.OpenAiLlmClient;
import com.systemforge.backend.architect.llm.impl.ResilientLlmClient;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for LLM clients used by the AI Architect.
 *
 * <p>Wraps the raw OpenAI client inside a {@link ResilientLlmClient} decorator
 * that adds circuit breaker and retry capabilities. The application context
 * only sees the resilient version — all consumers get fault-tolerant LLM calls
 * without any code changes.
 */
@Configuration
@Slf4j
public class LlmConfig {

    /**
     * Registers the resilient OpenAI LLM client.
     *
     * <p>Composition: OpenAIClient → OpenAiLlmClient → ResilientLlmClient
     * <ul>
     *   <li>OpenAiLlmClient handles the SDK integration (prompts, parsing)</li>
     *   <li>ResilientLlmClient wraps it with circuit breaker + retry</li>
     * </ul>
     */
    @Bean
    @ConditionalOnProperty(name = "llm.provider", havingValue = "openai", matchIfMissing = true)
    public LlmClient openAiClient(OpenAIClient openAIClient,
                                  CircuitBreakerRegistry cbRegistry,
                                  RetryRegistry retryRegistry,
                                  @org.springframework.beans.factory.annotation.Value("${openai.model:gpt-4}") String modelName) {
        OpenAiLlmClient rawClient = new OpenAiLlmClient(openAIClient, modelName);
        log.info("[LLM_CONFIG] Registering ResilientLlmClient wrapping OpenAI (circuit breaker + retry enabled)");
        return new ResilientLlmClient(rawClient, cbRegistry, retryRegistry);
    }
}
