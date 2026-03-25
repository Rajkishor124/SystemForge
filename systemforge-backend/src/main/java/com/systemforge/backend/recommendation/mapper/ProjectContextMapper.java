package com.systemforge.backend.recommendation.mapper;

import com.systemforge.backend.recommendation.dto.RecommendationRequest;
import com.systemforge.backend.recommendation.model.ProjectContext;
import org.springframework.stereotype.Component;

/**
 * Maps external DTO to internal ProjectContext.
 */
@Component
public class ProjectContextMapper {

    public ProjectContext toContext(RecommendationRequest request) {

        return ProjectContext.builder()
                .appType(request.getAppType())
                .appScale(request.getScale())
                .features(request.getFeatures())
                .region(request.getRegion())
                .expectedUsers(request.getExpectedUsers())
                .build();
    }
}