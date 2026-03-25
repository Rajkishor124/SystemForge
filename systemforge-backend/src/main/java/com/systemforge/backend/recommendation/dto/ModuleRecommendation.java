package com.systemforge.backend.recommendation.dto;

import com.systemforge.backend.common.enums.ModuleType;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Represents recommendation for a specific module (AUTH, PAYMENT, etc.)
 */
@Getter
@Builder
public class ModuleRecommendation {

    /**
     * Logical module (NOT user feature).
     */
    private final ModuleType module;

    /**
     * List of suggestions for this module.
     */
    private final List<RecommendationItem> recommendations;
}