package com.systemforge.backend.recommendation.rule.impl;

import com.systemforge.backend.common.enums.*;
import com.systemforge.backend.recommendation.dto.ModuleRecommendation;
import com.systemforge.backend.recommendation.dto.RecommendationItem;
import com.systemforge.backend.recommendation.model.ProjectContext;
import com.systemforge.backend.recommendation.rule.RecommendationRule;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Rule for notification system recommendations.
 */
@Component
public class NotificationRule implements RecommendationRule {

    @Override
    public boolean supports(ProjectContext context) {
        return context.hasFeature(FeatureType.NOTIFICATION)
                || context.isLargeScale();
    }

    @Override
    public Optional<ModuleRecommendation> apply(ProjectContext context) {

        RecommendationItem item;

        if (context.isLargeScale()) {
            item = RecommendationItem.builder()
                    .title("Use Event-Driven Notification System")
                    .description("Use Kafka → Notification Service → FCM/Email for scalability and reliability.")
                    .confidence(0.9)
                    .alternatives(List.of("Direct API-based notifications"))
                    .build();
        } else {
            item = RecommendationItem.builder()
                    .title("Use Firebase + Email Notifications")
                    .description("Simple and effective for small to medium scale systems.")
                    .confidence(0.85)
                    .alternatives(List.of("AWS SNS"))
                    .build();
        }

        return Optional.of(
                ModuleRecommendation.builder()
                        .module(ModuleType.NOTIFICATION)
                        .recommendations(List.of(item))
                        .build()
        );
    }

    @Override
    public int getPriority() {
        return 70;
    }
}