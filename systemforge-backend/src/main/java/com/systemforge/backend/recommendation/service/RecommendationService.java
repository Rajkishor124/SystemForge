package com.systemforge.backend.recommendation.service;

import com.systemforge.backend.recommendation.dto.RecommendationRequest;
import com.systemforge.backend.recommendation.dto.RecommendationResult;

/**
 * Recommendation Engine service contract.
 *
 * <p>This service acts as a thin orchestration layer:
 * <ul>
 *     <li>Converts request → ProjectContext</li>
 *     <li>Delegates execution to RecommendationEngine</li>
 * </ul>
 *
 * <p>The system is stateless and fully rule-driven,
 * making it easy to evolve into a microservice or AI-based engine.
 */
public interface RecommendationService {

    /**
     * Generates architecture recommendations.
     *
     * @param request user input context
     * @return structured recommendation result
     */
    RecommendationResult recommend(RecommendationRequest request);
}