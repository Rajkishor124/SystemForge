package com.systemforge.backend.recommendation.engine;

import com.systemforge.backend.common.enums.ModuleType;
import com.systemforge.backend.recommendation.dto.ModuleRecommendation;
import com.systemforge.backend.recommendation.dto.RecommendationItem;
import com.systemforge.backend.recommendation.dto.RecommendationResult;
import com.systemforge.backend.recommendation.model.ProjectContext;
import com.systemforge.backend.common.enums.AppScale;
import com.systemforge.backend.common.enums.AppType;
import com.systemforge.backend.recommendation.rule.RecommendationRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RecommendationEngineTest {

    private RecommendationEngine engine;
    private RecommendationRule mockRule;

    @BeforeEach
    void setUp() {
        mockRule = mock(RecommendationRule.class);
        engine = new RecommendationEngine(List.of(mockRule));
    }

    @Test
    void testGenerate_WithApplicableRule() {
        // Arrange
        ProjectContext context = ProjectContext.builder()
                .appType(AppType.SAAS)
                .appScale(AppScale.LARGE)
                .build();

        when(mockRule.supports(any(ProjectContext.class))).thenReturn(true);
        when(mockRule.getPriority()).thenReturn(10);
        
        RecommendationItem item = RecommendationItem.builder()
                .title("Use PostgreSQL")
                .confidence(0.9)
                .build();
                
        ModuleRecommendation moduleRec = ModuleRecommendation.builder()
                .module(ModuleType.DATABASE)
                .recommendations(List.of(item))
                .build();
                
        when(mockRule.apply(context)).thenReturn(Optional.of(moduleRec));

        // Act
        RecommendationResult result = engine.generate(context);

        // Assert
        assertNotNull(result);
        assertEquals(AppType.SAAS, result.getAppType());
        assertEquals(1, result.getModules().size());
        
        ModuleRecommendation actualModule = result.getModules().get(0);
        assertEquals(ModuleType.DATABASE, actualModule.getModule());
        assertEquals(1, actualModule.getRecommendations().size());
        
        RecommendationItem actualItem = actualModule.getRecommendations().get(0);
        assertEquals(9.0, actualItem.getScore(), 0.01); // 0.9 * 10 priority
    }
}
