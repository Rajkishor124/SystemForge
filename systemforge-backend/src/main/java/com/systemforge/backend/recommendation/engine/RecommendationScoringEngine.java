package com.systemforge.backend.recommendation.engine;

import com.systemforge.backend.recommendation.ai.model.AIRecommendation;
import com.systemforge.backend.recommendation.ai.model.ModuleScore;
import com.systemforge.backend.recommendation.dto.ModuleRecommendation;
import com.systemforge.backend.recommendation.dto.RecommendationItem;
import com.systemforge.backend.recommendation.model.ScoredModule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RecommendationScoringEngine {

    private final WeightedScoreCalculator calculator;

    /**
     * Scores and ranks modules using:
     * - Rule-based scores (aggregated from items)
     * - AI confidence scores (LLM-generated)
     */
    public List<ScoredModule> scoreModules(List<ModuleRecommendation> modules,
                                           AIRecommendation ai) {

        if (modules == null || modules.isEmpty()) {
            return Collections.emptyList();
        }

        return modules.stream()
                .map(module -> {

                    String moduleName = module.getModule().name();

                    double baseScore = calculateModuleScore(module);
                    double aiScore = getAiConfidence(moduleName, ai);

                    double finalScore = calculator.calculate(baseScore, aiScore);

                    return ScoredModule.builder()
                            .moduleName(moduleName)
                            .baseScore(baseScore)
                            .aiScore(aiScore)
                            .finalScore(finalScore)
                            .reason(buildReason(baseScore, aiScore))
                            .build();

                })
                .sorted((a, b) -> Double.compare(b.getFinalScore(), a.getFinalScore()))
                .toList();
    }

    /**
     * Aggregate rule-based scores from items.
     */
    private double calculateModuleScore(ModuleRecommendation module) {

        if (module.getRecommendations() == null || module.getRecommendations().isEmpty()) {
            return 0.5;
        }

        return module.getRecommendations().stream()
                .map(RecommendationItem::getScore)
                .filter(score -> score != null)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.5);
    }

    /**
     * 🔥 NEW: AI confidence-based scoring (REAL AI)
     */
    private double getAiConfidence(String moduleName, AIRecommendation ai) {

        if (ai == null || ai.getModuleScores() == null) {
            return 0.5;
        }

        return ai.getModuleScores().stream()
                .filter(m -> m.getModule() != null &&
                        m.getModule().equalsIgnoreCase(moduleName))
                .map(ModuleScore::getConfidence)
                .findFirst()
                .orElse(0.5);
    }

    /**
     * Debug / explainability support
     */
    private String buildReason(double baseScore, double aiScore) {
        return "RuleScore=" + baseScore + ", AIConfidence=" + aiScore;
    }
}
