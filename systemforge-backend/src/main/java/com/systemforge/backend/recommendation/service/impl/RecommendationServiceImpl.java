package com.systemforge.backend.recommendation.service.impl;

import com.systemforge.backend.recommendation.ai.RecommendationAIService;
import com.systemforge.backend.recommendation.dto.RecommendationRequest;
import com.systemforge.backend.recommendation.dto.RecommendationResult;
import com.systemforge.backend.recommendation.engine.RecommendationEngine;
import com.systemforge.backend.recommendation.mapper.ProjectContextMapper;
import com.systemforge.backend.recommendation.model.ProjectContext;
import com.systemforge.backend.recommendation.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Recommendation Service — delegates to rule engine and optionally enhances with AI.
 *
 * <p>When OpenAI is not configured, the service returns rule-based results only.
 */
@Service
@Slf4j
public class RecommendationServiceImpl implements RecommendationService {

    private final RecommendationEngine engine;
    private final ProjectContextMapper mapper;
    private final RecommendationAIService aiService;

    @Autowired
    public RecommendationServiceImpl(RecommendationEngine engine,
                                     ProjectContextMapper mapper,
                                     @Autowired(required = false) RecommendationAIService aiService) {
        this.engine = engine;
        this.mapper = mapper;
        this.aiService = aiService;
    }

    @Override
    public RecommendationResult recommend(RecommendationRequest request) {

        log.info("Generating recommendation for appType={} scale={}",
                request.getAppType(), request.getScale());

        // Step 1: Convert DTO → Domain Context
        ProjectContext context = mapper.toContext(request);

        // Step 2: Delegate to engine for base rules
        RecommendationResult baseResult = engine.generate(context);

        log.info("Base recommendation generated with {} modules",
                baseResult.getModules().size());

        // Step 3: Enhance with AI (if OpenAI is configured)
        if (aiService != null) {
            return aiService.enhance(context, baseResult);
        }

        log.warn("OpenAI not configured — returning rule-based result only");
        return baseResult;
    }
}