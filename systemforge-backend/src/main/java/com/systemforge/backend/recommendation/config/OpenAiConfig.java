package com.systemforge.backend.recommendation.config;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Conditionally creates the OpenAI client bean only when a valid API key is provided.
 * This allows the application to start without an OpenAI key for local development.
 */
@Configuration
public class OpenAiConfig {

    @Bean
    @ConditionalOnProperty(name = "spring.ai.openai.api-key", matchIfMissing = false)
    public OpenAIClient openAIClient(@Value("${spring.ai.openai.api-key}") String apiKey) {
        return OpenAIOkHttpClient.builder()
                .apiKey(apiKey)
                .build();
    }
}
