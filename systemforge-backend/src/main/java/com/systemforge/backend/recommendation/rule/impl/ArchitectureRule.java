package com.systemforge.backend.recommendation.rule.impl;

import com.systemforge.backend.common.enums.ModuleType;
import com.systemforge.backend.recommendation.dto.ModuleRecommendation;
import com.systemforge.backend.recommendation.dto.RecommendationItem;
import com.systemforge.backend.recommendation.model.ProjectContext;
import com.systemforge.backend.recommendation.rule.RecommendationRule;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Rule for system architecture recommendations.
 */
@Component
public class ArchitectureRule implements RecommendationRule {

    @Override
    public boolean supports(ProjectContext context) {
        return true; // Always applies
    }

    @Override
    public Optional<ModuleRecommendation> apply(ProjectContext context) {

        List<RecommendationItem> items = new ArrayList<>();

        // ========================
        // Base Architecture
        // ========================

        if (context.isSmallScale()) {
            items.add(RecommendationItem.builder()
                    .title("Use Modular Monolith Architecture")
                    .description("Best for fast development and low operational overhead.")
                    .confidence(0.95)
                    .alternatives(List.of("Microservices"))
                    .build());
        }

        if (context.isMediumScale()) {
            items.add(RecommendationItem.builder()
                    .title("Start with Modular Monolith, Prepare for Microservices")
                    .description("Balance between simplicity and scalability.")
                    .confidence(0.9)
                    .alternatives(List.of("Direct microservices"))
                    .build());

            items.add(RecommendationItem.builder()
                    .title("Introduce Redis Caching")
                    .description("Improves performance and reduces DB load.")
                    .confidence(0.85)
                    .alternatives(List.of("No caching"))
                    .build());
        }

        if (context.isLargeScale()) {
            items.add(RecommendationItem.builder()
                    .title("Adopt Microservices Architecture")
                    .description("Supports independent scaling and high availability.")
                    .confidence(0.95)
                    .alternatives(List.of("Modular monolith"))
                    .build());

            items.add(RecommendationItem.builder()
                    .title("Use Event-Driven Architecture (Kafka)")
                    .description("Handles high throughput and async communication.")
                    .confidence(0.9)
                    .alternatives(List.of("Synchronous REST"))
                    .build());
        }

        return Optional.of(
                ModuleRecommendation.builder()
                        .module(ModuleType.ARCHITECTURE)
                        .recommendations(items)
                        .build()
        );
    }

    @Override
    public int getPriority() {
        return 90;
    }
}