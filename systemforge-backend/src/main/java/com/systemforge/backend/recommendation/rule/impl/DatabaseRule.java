package com.systemforge.backend.recommendation.rule.impl;

import com.systemforge.backend.common.enums.*;
import com.systemforge.backend.recommendation.dto.ModuleRecommendation;
import com.systemforge.backend.recommendation.dto.RecommendationItem;
import com.systemforge.backend.recommendation.model.ProjectContext;
import com.systemforge.backend.recommendation.rule.RecommendationRule;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Rule for database recommendations.
 */
@Component
public class DatabaseRule implements RecommendationRule {

    @Override
    public boolean supports(ProjectContext context) {
        return true; // Always required
    }

    @Override
    public Optional<ModuleRecommendation> apply(ProjectContext context) {

        List<RecommendationItem> items = new ArrayList<>();

        if (context.isLargeScale()) {
            items.add(RecommendationItem.builder()
                    .title("Use PostgreSQL with Read Replicas")
                    .description("Supports scaling with replication and better performance for complex queries.")
                    .confidence(0.93)
                    .alternatives(List.of("MySQL with replicas", "Aurora"))
                    .build());

            items.add(RecommendationItem.builder()
                    .title("Use Redis for Hot Data Caching")
                    .description("Reduces database load and improves latency.")
                    .confidence(0.9)
                    .alternatives(List.of("No caching"))
                    .build());

        } else {
            items.add(RecommendationItem.builder()
                    .title("Use MySQL (Relational Database)")
                    .description("Reliable and sufficient for most transactional systems.")
                    .confidence(0.9)
                    .alternatives(List.of("PostgreSQL", "MongoDB (for flexible schema)"))
                    .build());
        }

        return Optional.of(
                ModuleRecommendation.builder()
                        .module(ModuleType.DATABASE)
                        .recommendations(items)
                        .build()
        );
    }

    @Override
    public int getPriority() {
        return 95;
    }
}