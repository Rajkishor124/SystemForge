package com.systemforge.backend.recommendation.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ScoredModule {

    String moduleName;

    double baseScore;     // from rule engine
    double aiScore;       // from AI
    double finalScore;    // weighted

    String reason;
}
