package com.systemforge.backend.architect.llm.impl;

import com.openai.client.OpenAIClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.StructuredChatCompletionCreateParams;
import com.systemforge.backend.architect.llm.LlmClient;
import com.systemforge.backend.architect.llm.LlmResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

/**
 * OpenAI implementation of the LLM client.
 *
 * <p>Wraps the existing OpenAI Java SDK with:
 * <ul>
 *     <li>Structured logging (latency, tokens, model)</li>
 *     <li>Graceful error handling</li>
 *     <li>System/user message separation</li>
 * </ul>
 */
@Slf4j
public class OpenAiLlmClient implements LlmClient {

    private final OpenAIClient openAIClient;
    private final String modelName;

    public OpenAiLlmClient(OpenAIClient openAIClient, String modelName) {
        this.openAIClient = openAIClient;
        this.modelName = modelName;
    }

    @Override
    public LlmResponse complete(String systemPrompt, String userPrompt) {
        long startTime = System.currentTimeMillis();

        try {
            ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                    .model(modelName)
                    .addSystemMessage(systemPrompt)
                    .addUserMessage(userPrompt)
                    .build();

            ChatCompletion completion = openAIClient.chat().completions().create(params);

            String content = completion.choices().stream()
                    .findFirst()
                    .flatMap(choice -> choice.message().content())
                    .orElse("");

            long latency = System.currentTimeMillis() - startTime;
            int promptTokens = completion.usage().map(u -> (int) u.promptTokens()).orElse(0);
            int completionTokens = completion.usage().map(u -> (int) u.completionTokens()).orElse(0);

            log.info("[ARCHITECT_LLM] model={}, promptTokens={}, completionTokens={}, latencyMs={}",
                    modelName, promptTokens, completionTokens, latency);

            return LlmResponse.builder()
                    .content(content)
                    .model(modelName)
                    .promptTokens(promptTokens)
                    .completionTokens(completionTokens)
                    .latencyMs(latency)
                    .fallback(false)
                    .build();

        } catch (Exception e) {
            long latency = System.currentTimeMillis() - startTime;
            log.error("[ARCHITECT_LLM] OpenAI call failed after {}ms: {}", latency, e.getMessage());
            throw new RuntimeException("LLM call failed: " + e.getMessage(), e);
        }
    }

    @Override
    public <T> T completeStructured(String systemPrompt, String userPrompt, Class<T> responseType) {
        long startTime = System.currentTimeMillis();

        try {
            StructuredChatCompletionCreateParams<T> params =
                    StructuredChatCompletionCreateParams.<T>builder()
                            .model(modelName)
                            .addSystemMessage(systemPrompt)
                            .addUserMessage(userPrompt)
                            .responseFormat(responseType)
                            .build();

            T result = openAIClient.chat()
                    .completions()
                    .create(params)
                    .choices()
                    .stream()
                    .findFirst()
                    .flatMap(choice -> choice.message().content())
                    .orElseThrow(() -> new RuntimeException("Empty structured response from LLM"));

            long latency = System.currentTimeMillis() - startTime;
            log.info("[ARCHITECT_LLM_STRUCTURED] model={}, type={}, latencyMs={}",
                    modelName, responseType.getSimpleName(), latency);

            return result;

        } catch (Exception e) {
            long latency = System.currentTimeMillis() - startTime;
            log.error("[ARCHITECT_LLM_STRUCTURED] failed after {}ms: {}", latency, e.getMessage());
            throw new RuntimeException("Structured LLM call failed: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isAvailable() {
        return openAIClient != null;
    }
}
