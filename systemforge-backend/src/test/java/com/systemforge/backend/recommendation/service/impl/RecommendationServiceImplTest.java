package com.systemforge.backend.recommendation.service.impl;

import com.systemforge.backend.recommendation.ai.RecommendationAIService;
import com.systemforge.backend.recommendation.dto.RecommendationRequest;
import com.systemforge.backend.recommendation.dto.RecommendationResult;
import com.systemforge.backend.recommendation.engine.RecommendationEngine;
import com.systemforge.backend.recommendation.mapper.ProjectContextMapper;
import com.systemforge.backend.recommendation.model.ProjectContext;
import com.systemforge.backend.common.enums.AppScale;
import com.systemforge.backend.common.enums.AppType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecommendationServiceImplTest {

    @Mock
    private RecommendationEngine engine;

    @Mock
    private RecommendationAIService aiService;

    @Mock
    private ProjectContextMapper mapper;

    @InjectMocks
    private RecommendationServiceImpl service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testRecommend_OrchestratesExecution() {
        // Arrange
        RecommendationRequest request = new RecommendationRequest();
        request.setAppType(AppType.SAAS);
        request.setScale(AppScale.LARGE);

        ProjectContext context = ProjectContext.builder()
                .appType(AppType.SAAS)
                .appScale(AppScale.LARGE)
                .build();

        RecommendationResult baseResult = RecommendationResult.builder()
                .appType(AppType.SAAS)
                .architectureSummary("Base Model")
                .modules(java.util.Collections.emptyList())
                .build();

        RecommendationResult enhancedResult = RecommendationResult.builder()
                .appType(AppType.SAAS)
                .architectureSummary("AI Enhanced Model")
                .modules(java.util.Collections.emptyList())
                .build();

        when(mapper.toContext(any(RecommendationRequest.class))).thenReturn(context);
        when(engine.generate(any(ProjectContext.class))).thenReturn(baseResult);
        when(aiService.enhance(any(ProjectContext.class), any(RecommendationResult.class))).thenReturn(enhancedResult);

        // Act
        RecommendationResult finalResult = service.recommend(request);

        // Assert
        assertNotNull(finalResult);
        assertEquals("AI Enhanced Model", finalResult.getArchitectureSummary());
        
        verify(mapper).toContext(request);
        verify(engine).generate(context);
        verify(aiService).enhance(context, baseResult);
    }
}
