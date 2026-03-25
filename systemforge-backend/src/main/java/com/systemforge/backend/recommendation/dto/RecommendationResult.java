package com.systemforge.backend.recommendation.dto;

import com.systemforge.backend.common.enums.AppScale;
import com.systemforge.backend.common.enums.AppType;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Final structured output from Recommendation Engine.
 *
 * <p>Organized by modules (Auth, Architecture, Payment, etc.)
 * instead of system-centric output.
 */
@Getter
@Builder
public class RecommendationResult {

    private final AppType appType;
    private final AppScale appScale;

    /**
     * High-level architecture summary.
     */
    private final String architectureSummary;

    /**
     * Module-wise recommendations.
     */
    private final List<ModuleRecommendation> modules;

    private List<String> aiImprovements;
    private List<String> aiTradeoffs;

}