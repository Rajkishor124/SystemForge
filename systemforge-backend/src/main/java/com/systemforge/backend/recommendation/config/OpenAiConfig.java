package com.systemforge.backend.recommendation.config;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Conditionally creates the OpenAI client bean only when a valid API key is provided.
 * This allows the application to start without an OpenAI key for local development.
 */
@Configuration
@Slf4j
public class OpenAiConfig {

    @Bean
    @ConditionalOnProperty(name = "spring.ai.openai.api-key", matchIfMissing = false)
    public OpenAIClient openAIClient(
            @Value("${spring.ai.openai.api-key}") String apiKey,
            @Value("${spring.ai.openai.base-url:}") String baseUrl) {

        OpenAIOkHttpClient.Builder builder = OpenAIOkHttpClient.builder()
                .apiKey(apiKey);

        // Support OpenRouter, Azure, or any OpenAI-compatible provider
        if (baseUrl != null && !baseUrl.isBlank()) {
            builder.baseUrl(baseUrl);
            log.info("[OPENAI_CONFIG] Using custom base URL: {}", baseUrl);
        } else {
            log.info("[OPENAI_CONFIG] Using default OpenAI base URL");
        }

        return builder.build();
    }
}
