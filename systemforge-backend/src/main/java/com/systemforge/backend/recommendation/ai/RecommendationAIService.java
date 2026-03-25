package com.systemforge.backend.recommendation.ai;

import com.systemforge.backend.recommendation.dto.RecommendationResult;
import com.systemforge.backend.recommendation.model.ProjectContext;

/**
 * AI layer for enhancing recommendations.
 */
public interface RecommendationAIService {

    /**
     * Enhances rule-based result using AI.
     */
    RecommendationResult enhance(ProjectContext context,
                                 RecommendationResult baseResult);
}