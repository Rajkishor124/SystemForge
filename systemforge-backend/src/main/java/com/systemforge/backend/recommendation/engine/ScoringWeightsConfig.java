package com.systemforge.backend.recommendation.engine;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Component
public class ScoringWeightsConfig {

    @Value("${recommendation.weight.base:0.6}")
    private double baseWeight;

    @Value("${recommendation.weight.ai:0.4}")
    private double aiWeight;
}
