package com.systemforge.backend.recommendation.dto;

import com.systemforge.backend.common.enums.AppScale;
import com.systemforge.backend.common.enums.AppType;
import com.systemforge.backend.common.enums.FeatureType;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Set;

/**
 * Incoming request for system design recommendations.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationRequest {

    /**
     * Type of application user is building.
     */
    @NotNull(message = "App type is required")
    private AppType appType;

    /**
     * Expected scale of the system.
     */
    @NotNull(message = "App scale is required")
    private AppScale scale;

    /**
     * Features user wants in the system.
     */
    private Set<FeatureType> features;

    /**
     * Optional: target region
     */
    private String region;

    /**
     * Optional: expected concurrent users
     */
    private Integer expectedUsers;
}