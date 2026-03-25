package com.systemforge.backend.recommendation.engine;

import com.systemforge.backend.common.enums.ModuleType;
import com.systemforge.backend.recommendation.dto.ModuleRecommendation;
import com.systemforge.backend.recommendation.dto.RecommendationItem;
import com.systemforge.backend.recommendation.dto.RecommendationResult;
import com.systemforge.backend.recommendation.model.ProjectContext;
import com.systemforge.backend.recommendation.rule.RecommendationRule;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Advanced Recommendation Engine with weighted scoring.
 */
@Component
public class RecommendationEngine {

    private final List<RecommendationRule> rules;

    public RecommendationEngine(List<RecommendationRule> rules) {
        this.rules = rules;
    }

    public RecommendationResult generate(ProjectContext context) {

        // 1. Sort rules by priority
        List<RecommendationRule> sortedRules = rules.stream()
                .sorted(Comparator.comparingInt(RecommendationRule::getPriority).reversed())
                .toList();

        Map<ModuleType, List<RecommendationItem>> moduleMap = new LinkedHashMap<>();

        // 2. Execute rules + apply weighted scoring
        for (RecommendationRule rule : sortedRules) {

            if (!rule.supports(context)) continue;

            int priority = rule.getPriority();

            rule.apply(context).ifPresent(moduleRec -> {

                List<RecommendationItem> scoredItems = moduleRec.getRecommendations().stream()
                        .map(item -> {

                            double score = item.getConfidence() * priority;

                            return item.toBuilder()
                                    .score(score)
                                    .build();
                        })
                        .collect(Collectors.toList());

                moduleMap.computeIfAbsent(moduleRec.getModule(), k -> new ArrayList<>())
                        .addAll(scoredItems);
            });
        }

        // 3. Sort items inside each module by score
        List<ModuleRecommendation> modules = moduleMap.entrySet().stream()
                .map(entry -> {

                    List<RecommendationItem> sortedItems = entry.getValue().stream()
                            .sorted(Comparator.comparingDouble(RecommendationItem::getScore).reversed())
                            .collect(Collectors.toList());

                    return ModuleRecommendation.builder()
                            .module(entry.getKey())
                            .recommendations(sortedItems)
                            .build();
                })
                .collect(Collectors.toList());

        // 4. Build final response
        RecommendationResult baseResult = RecommendationResult.builder()
                .appType(context.getAppType())
                .appScale(context.getAppScale())
                .modules(modules)
                .architectureSummary("Base rule-generated architecture")
                .build();

        return baseResult;
    }
}