package com.systemforge.backend.architect.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Structured requirements parsed from user input.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ParsedRequirements {

    private String appType;
    private String appScale;
    private List<String> primaryFeatures;
    private List<String> nonFunctionalRequirements;
    private List<String> constraints;
    private int estimatedConcurrentUsers;
    private String summary;
}
