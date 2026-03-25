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
 * Rule for authentication recommendations.
 */
@Component
public class AuthRule implements RecommendationRule {

    @Override
    public boolean supports(ProjectContext context) {
        return context.hasFeature(FeatureType.AUTH);
    }

    @Override
    public Optional<ModuleRecommendation> apply(ProjectContext context) {

        List<RecommendationItem> items = new ArrayList<>();

        // ========================
        // Strategy Selection
        // ========================

        if (context.isAppType(AppType.RIDE_HAILING)
                || context.isAppType(AppType.FOOD_DELIVERY)
                || context.isAppType(AppType.SOCIAL_MEDIA)) {

            items.add(RecommendationItem.builder()
                    .title("Use OTP-based Authentication")
                    .description("Mobile-first apps benefit from OTP login for frictionless onboarding.")
                    .confidence(0.9)
                    .alternatives(List.of("Email + Password"))
                    .build());
        } else {
            items.add(RecommendationItem.builder()
                    .title("Use Email + Password Authentication")
                    .description("Standard approach for SaaS and enterprise applications.")
                    .confidence(0.85)
                    .alternatives(List.of("OAuth2 / Social Login"))
                    .build());
        }

        // ========================
        // Token Strategy
        // ========================

        items.add(RecommendationItem.builder()
                .title("Use JWT with Refresh Tokens")
                .description("Ensures stateless authentication and scalable session handling.")
                .confidence(0.95)
                .alternatives(List.of("Session-based auth"))
                .build());

        // ========================
        // Security Enhancement
        // ========================

        if (context.isLargeScale()) {
            items.add(RecommendationItem.builder()
                    .title("Enable Multi-Factor Authentication (MFA)")
                    .description("Large-scale systems require stronger security layers.")
                    .confidence(0.8)
                    .alternatives(List.of("OTP only"))
                    .build());
        }

        return Optional.of(
                ModuleRecommendation.builder()
                        .module(ModuleType.AUTH)
                        .recommendations(items)
                        .build()
        );
    }

    @Override
    public int getPriority() {
        return 100; // critical system
    }
}