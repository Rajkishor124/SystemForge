package com.systemforge.backend.recommendation.controller;

import com.systemforge.backend.common.dto.ApiResponse;
import com.systemforge.backend.recommendation.dto.RecommendationRequest;
import com.systemforge.backend.recommendation.dto.RecommendationResult;
import com.systemforge.backend.recommendation.service.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for architecture recommendations.
 *
 * <p>This controller is intentionally thin:
 * - No business logic
 * - Delegates to service layer
 * - Handles request/response contract only
 */
@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
@Tag(name = "Recommendation Engine", description = "Generates system design recommendations based on app context")
public class RecommendationController {

    private final RecommendationService recommendationService;

    @PostMapping
    @Operation(
            summary = "Generate architecture recommendations",
            description = """
                    Accepts application context (type, scale, features)
                    and returns structured system design recommendations.

                    Example:
                    - Auth strategy
                    - Architecture pattern
                    - Scaling suggestions
                    - Infra recommendations
                    """
    )
    public ResponseEntity<ApiResponse<RecommendationResult>> recommend(
            @Valid @RequestBody RecommendationRequest request
    ) {

        RecommendationResult result = recommendationService.recommend(request);

        return ResponseEntity.ok(
                ApiResponse.success("Recommendations generated successfully", result)
        );
    }
}