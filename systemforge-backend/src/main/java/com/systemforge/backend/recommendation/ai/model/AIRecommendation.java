package com.systemforge.backend.recommendation.ai.model;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.Getter;

import java.util.List;

/**
 * Structured AI output for schema-based response.
 */
@Getter
public class AIRecommendation {

    @JsonPropertyDescription("High-level architecture summary")
    private String summary;

    @JsonPropertyDescription("Suggested improvements")
    private List<String> improvements;

    @JsonPropertyDescription("Tradeoffs of the architecture")
    private List<String> tradeoffs;

    @JsonPropertyDescription("Confidence score per module")
    private List<ModuleScore> moduleScores;
}
