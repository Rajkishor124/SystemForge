package com.systemforge.backend.recommendation.ai.prompt;

import com.systemforge.backend.recommendation.dto.RecommendationResult;
import com.systemforge.backend.recommendation.model.ProjectContext;
import org.springframework.stereotype.Component;

@Component
public class RecommendationPromptBuilder {

    public String build(ProjectContext context, RecommendationResult result) {

        return """
            You are a senior backend architect.

            Given the following system design recommendations,
            return a STRICT JSON response only.

            App Type: %s
            Scale: %s

            Modules:
            %s

            JSON FORMAT:
            {
              "summary": "string",
              "improvements": ["string"],
              "tradeoffs": ["string"],
              "moduleScores": [
                {
                  "module": "AUTH",
                  "confidence": 0.9
                }
              ]
            }

            RULES:
            - confidence must be between 0 and 1
            - include ALL modules listed
            - module must match exactly (AUTH, PAYMENT, etc.)
            - return ONLY JSON
            - no explanations
            - no markdown
            - no extra text
            """.formatted(
                context.getAppType(),
                context.getAppScale(),
                result.getModules()
        );
    }
}
