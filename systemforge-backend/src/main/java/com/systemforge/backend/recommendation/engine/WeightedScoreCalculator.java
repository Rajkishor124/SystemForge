package com.systemforge.backend.recommendation.engine;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WeightedScoreCalculator {

    private final ScoringWeightsConfig weights;

    public double calculate(double baseScore, double aiScore) {
        return (baseScore * weights.getBaseWeight())
                + (aiScore * weights.getAiWeight());
    }
}
