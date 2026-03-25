package com.systemforge.backend.recommendation.ai.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.systemforge.backend.recommendation.ai.model.AIRecommendation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AIResponseParser {

    private final ObjectMapper objectMapper;

    public AIRecommendation parse(String json) {

        try {
            return objectMapper.readValue(json, AIRecommendation.class);
        } catch (Exception ex) {
            log.error("Failed to parse AI response: {}", json, ex);
            throw new RuntimeException("Invalid AI response format");
        }
    }
}
