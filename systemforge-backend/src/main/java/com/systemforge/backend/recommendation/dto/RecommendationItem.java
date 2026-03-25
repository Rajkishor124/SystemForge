package com.systemforge.backend.recommendation.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Atomic recommendation unit.
 */
@Getter
@Builder(toBuilder = true)
public class RecommendationItem {

    private final String title;
    private final String description;

    /**
     * Confidence (0–1)
     */
    private final double confidence;

    /**
     * Final weighted score (computed by engine)
     */
    private final double score;

    private final List<String> alternatives;
}