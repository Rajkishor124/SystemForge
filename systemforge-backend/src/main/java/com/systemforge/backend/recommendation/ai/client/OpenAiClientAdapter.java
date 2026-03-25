package com.systemforge.backend.recommendation.ai.client;

import com.openai.client.OpenAIClient;
import com.openai.models.chat.completions.StructuredChatCompletionCreateParams;
import com.systemforge.backend.recommendation.exception.AiServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnBean(OpenAIClient.class)
@RequiredArgsConstructor
public class OpenAiClientAdapter {

    private final OpenAIClient openAIClient;

    @Value("${openai.model:gpt-4}")
    private String modelName;

    /**
     * Structured AI call (schema-based).
     */
    public <T> T getStructuredCompletion(String prompt, Class<T> responseType) {

        StructuredChatCompletionCreateParams<T> params =
                StructuredChatCompletionCreateParams.<T>builder()
                        .addUserMessage(prompt)
                        .model(modelName)
                        .responseFormat(responseType)
                        .build();

        return openAIClient.chat()
                .completions()
                .create(params)
                .choices()
                .stream()
                .findFirst()
                .flatMap(choice -> choice.message().content())
                .orElseThrow(() -> new AiServiceException("Empty AI response"));
    }
}
