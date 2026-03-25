package com.systemforge.backend.recommendation.ai.model;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.Getter;

@Getter
public class ModuleScore {

    @JsonPropertyDescription("Module name (AUTH, PAYMENT, etc.)")
    private String module;

    @JsonPropertyDescription("Confidence score between 0 and 1")
    private double confidence;
}
