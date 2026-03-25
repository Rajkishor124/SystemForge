package com.systemforge.backend.recommendation.rule;

import com.systemforge.backend.recommendation.dto.ModuleRecommendation;
import com.systemforge.backend.recommendation.model.ProjectContext;

import java.util.Optional;

/**
 * Core contract for all recommendation rules.
 *
 * <p>Each rule:
 * <ul>
 *     <li>Evaluates if it applies to a given context</li>
 *     <li>Generates module-specific recommendations</li>
 * </ul>
 *
 * <p>Designed using Open/Closed Principle:
 * Add new rules without modifying existing ones.
 */
public interface RecommendationRule {

    /**
     * Determines whether this rule should be applied.
     */
    boolean supports(ProjectContext context);

    /**
     * Executes the rule and returns recommendations.
     */
    Optional<ModuleRecommendation> apply(ProjectContext context);

    /**
     * Rule priority (higher = more important)
     */
    default int getPriority() {
        return 0;
    }
}