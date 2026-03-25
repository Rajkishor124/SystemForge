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
 * Rule for payment system recommendations.
 */
@Component
public class PaymentRule implements RecommendationRule {

    @Override
    public boolean supports(ProjectContext context) {
        return context.hasFeature(FeatureType.PAYMENT)
                || isPaymentRelevant(context.getAppType());
    }

    @Override
    public Optional<ModuleRecommendation> apply(ProjectContext context) {

        String gateway;

        if (context.getAppType() == AppType.ECOMMERCE
                || context.getAppType() == AppType.MARKETPLACE) {

            gateway = "Razorpay (India) / Stripe (Global) with webhook-based reconciliation";

        } else {
            gateway = "Razorpay with UPI + wallet support";
        }

        RecommendationItem item = RecommendationItem.builder()
                .title("Use Reliable Payment Gateway")
                .description(gateway + ". Always verify payments via webhooks and use idempotency keys.")
                .confidence(0.9)
                .alternatives(List.of("PayU", "Cashfree", "Paytm PG"))
                .build();

        return Optional.of(
                ModuleRecommendation.builder()
                        .module(ModuleType.PAYMENT)
                        .recommendations(List.of(item))
                        .build()
        );
    }

    private boolean isPaymentRelevant(AppType type) {
        return switch (type) {
            case RIDE_HAILING, ECOMMERCE, FOOD_DELIVERY, FINTECH,
                 MARKETPLACE, EDTECH -> true;
            default -> false;
        };
    }

    @Override
    public int getPriority() {
        return 80;
    }
}