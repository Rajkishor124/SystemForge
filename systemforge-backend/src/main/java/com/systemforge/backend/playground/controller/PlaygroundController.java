package com.systemforge.backend.playground.controller;

import com.systemforge.backend.common.dto.ApiResponse;
import com.systemforge.backend.playground.dto.PlaygroundConfigRequest;
import com.systemforge.backend.playground.dto.PlaygroundGeneratedOutput;
import com.systemforge.backend.playground.enums.FeatureToggle;
import com.systemforge.backend.playground.enums.ServiceType;
import com.systemforge.backend.playground.enums.ServiceVariant;
import com.systemforge.backend.playground.service.PlaygroundService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/playground")
@RequiredArgsConstructor
@Tag(name = "Microservices Playground", description = "Config-driven microservice architecture generator")
public class PlaygroundController {

    private final PlaygroundService playgroundService;

    @GetMapping("/services")
    @Operation(summary = "List all available service types")
    public ResponseEntity<ApiResponse<List<ServiceType>>> getServiceTypes() {
        return ResponseEntity.ok(
                ApiResponse.success("Service types loaded", playgroundService.getServiceTypes()));
    }

    @GetMapping("/services/{type}/variants")
    @Operation(summary = "List variants for a service type")
    public ResponseEntity<ApiResponse<List<ServiceVariant>>> getVariants(
            @PathVariable ServiceType type) {
        return ResponseEntity.ok(
                ApiResponse.success("Variants loaded", playgroundService.getVariants(type)));
    }

    @GetMapping("/services/{type}/variants/{variant}/features")
    @Operation(summary = "List compatible features for a service type + variant")
    public ResponseEntity<ApiResponse<List<FeatureToggle>>> getFeatures(
            @PathVariable ServiceType type,
            @PathVariable ServiceVariant variant) {
        return ResponseEntity.ok(
                ApiResponse.success("Features loaded", playgroundService.getFeatures(type, variant)));
    }

    @PostMapping("/generate")
    @Operation(summary = "Generate architecture from config (idempotent, cached)")
    public ResponseEntity<ApiResponse<PlaygroundGeneratedOutput>> generate(
            @Valid @RequestBody PlaygroundConfigRequest request) {
        PlaygroundGeneratedOutput output = playgroundService.generate(request);
        return ResponseEntity.ok(ApiResponse.success("Architecture generated", output));
    }

    @GetMapping("/history")
    @Operation(summary = "Get the authenticated user's generation history")
    public ResponseEntity<ApiResponse<List<PlaygroundGeneratedOutput>>> getHistory() {
        return ResponseEntity.ok(
                ApiResponse.success("History loaded", playgroundService.getHistory()));
    }
}
