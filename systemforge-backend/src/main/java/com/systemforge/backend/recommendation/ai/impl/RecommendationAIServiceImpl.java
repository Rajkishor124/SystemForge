package com.systemforge.backend.recommendation.ai.impl;

import com.systemforge.backend.recommendation.ai.RecommendationAIService;
import com.systemforge.backend.recommendation.ai.client.OpenAiClientAdapter;
import com.systemforge.backend.recommendation.ai.model.AIRecommendation;
import com.systemforge.backend.recommendation.ai.prompt.RecommendationPromptBuilder;
import com.systemforge.backend.recommendation.dto.ModuleRecommendation;
import com.systemforge.backend.recommendation.dto.RecommendationResult;
import com.systemforge.backend.recommendation.engine.RecommendationScoringEngine;
import com.systemforge.backend.recommendation.model.ProjectContext;
import com.systemforge.backend.recommendation.model.ScoredModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@ConditionalOnBean(OpenAiClientAdapter.class)
@RequiredArgsConstructor
@Slf4j
public class RecommendationAIServiceImpl implements RecommendationAIService {

    private final OpenAiClientAdapter openAiClientAdapter;
    private final RecommendationPromptBuilder promptBuilder;
    private final RecommendationScoringEngine scoringEngine;

    @Override
    public RecommendationResult enhance(ProjectContext context,
                                        RecommendationResult baseResult) {

        try {
            log.info("Starting AI enhancement for appType={}, scale={}",
                    context.getAppType(), context.getAppScale());

            // 1. Build prompt
            String prompt = promptBuilder.build(context, baseResult);

            // 2. Call AI (structured)
            AIRecommendation ai = openAiClientAdapter
                    .getStructuredCompletion(prompt, AIRecommendation.class);

            // 3. Score modules
            List<ScoredModule> scoredModules =
                    scoringEngine.scoreModules(baseResult.getModules(), ai);

            // 4. Merge results
            RecommendationResult result = merge(baseResult, ai, scoredModules);

            log.info("AI enhancement completed successfully");

            return result;

        } catch (Exception ex) {
            log.error("AI enhancement failed. Falling back to base result.", ex);
            return baseResult;
        }
    }

    /**
     * Merge rule-based + AI + scoring results
     */
    private RecommendationResult merge(RecommendationResult base,
                                       AIRecommendation ai,
                                       List<ScoredModule> scoredModules) {

        List<ModuleRecommendation> rankedModules = scoredModules.stream()
                .map(scored -> base.getModules().stream()
                        .filter(module ->
                                module.getModule().name()
                                        .equals(scored.getModuleName()))
                        .findFirst()
                        .orElse(null)
                )
                .filter(module -> module != null)
                .toList();

        return RecommendationResult.builder()
                .appType(base.getAppType())
                .appScale(base.getAppScale())
                .modules(rankedModules)
                .architectureSummary(ai.getSummary())
                .aiImprovements(ai.getImprovements())
                .aiTradeoffs(ai.getTradeoffs())
                .build();
    }
}
